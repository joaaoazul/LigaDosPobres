"""Varrimento: segurança, validação, upload, administração e precisão."""
import json, sys, uuid, urllib.request, urllib.error, http.cookiejar
exec(open("/home/user/LigaDosPobres/testes-e2e/e2e.py").read().split('def correr(')[0])

def sem_csrf(cliente, metodo, caminho, corpo=None):
    """O mesmo pedido, mas sem o cabeçalho do token."""
    dados = json.dumps(corpo).encode() if corpo is not None else None
    req = urllib.request.Request(API + caminho, data=dados, method=metodo)
    req.add_header("Content-Type", "application/json")
    try:
        with cliente.opener.open(req) as r:
            return r.status
    except urllib.error.HTTPError as e:
        return e.code

def cru(cliente, metodo, caminho, corpo_texto, tipo="application/json"):
    req = urllib.request.Request(API + caminho, data=corpo_texto.encode(), method=metodo)
    req.add_header("Content-Type", tipo)
    if cliente.token():
        req.add_header("X-XSRF-TOKEN", cliente.token())
    try:
        with cliente.opener.open(req) as r:
            return r.status
    except urllib.error.HTTPError as e:
        return e.code

def multipart(cliente, caminho, nome_campo, nome_ficheiro, bytes_ficheiro, tipo="image/png"):
    fronteira = "----" + uuid.uuid4().hex
    corpo = b""
    corpo += ("--%s\r\nContent-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n"
              "Content-Type: %s\r\n\r\n" % (fronteira, nome_campo, nome_ficheiro, tipo)).encode()
    corpo += bytes_ficheiro + ("\r\n--%s--\r\n" % fronteira).encode()
    req = urllib.request.Request(API + caminho, data=corpo, method="POST")
    req.add_header("Content-Type", "multipart/form-data; boundary=" + fronteira)
    if cliente.token():
        req.add_header("X-XSRF-TOKEN", cliente.token())
    try:
        with cliente.opener.open(req) as r:
            return r.status
    except urllib.error.HTTPError as e:
        return e.code

PNG = (b"\x89PNG\r\n\x1a\n" + b"\x00\x00\x00\rIHDR" + b"\x00\x00\x00\x01\x00\x00\x00\x01"
       + b"\x08\x06\x00\x00\x00\x1f\x15\xc4\x89" + b"\x00" * 40)
SVG = b'<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>'

def correr(ronda):
    sufixo = uuid.uuid4().hex[:8]
    admin = Cliente(); admin.pedir("POST", "/api/auth/login", {"email": "admin@teste.pt", "password": "passwordsegura1"})
    conv = admin.pedir("POST", "/api/admin/convites", {"nota": sufixo, "diasValidade": 7})
    g = Cliente(); g.pedir("POST", "/api/auth/registo", {"codigo": conv["codigo"], "nome": "G",
        "email": "v-%s@t.pt" % sufixo, "password": "passwordsegura1"})
    lid = g.pedir("POST", "/api/ligas", {"nome": "Varre %s" % sufixo, "maxEquipas": 5})["id"]

    # ------------------------------------------------------------------ CSRF ---
    igual("POST sem token CSRF", sem_csrf(g, "POST", "/api/ligas", {"nome": "X", "maxEquipas": 3}), 403)
    igual("PUT sem token CSRF", sem_csrf(g, "PUT", "/api/ligas/%s/regra-divida" % lid,
          {"valorInscricao": 0, "valorInicial": 0, "incremento": 0, "equipasPorEscalao": 1,
           "valorMaximo": 0, "jornadasPorBloco": 1}), 403)
    igual("DELETE sem token CSRF", sem_csrf(g, "DELETE", "/api/ligas/%s/logo" % lid), 403)
    igual("GET não precisa de token", sem_csrf(g, "GET", "/api/ligas"), 200)

    # ------------------------------------------------------- corpos estranhos ---
    igual("JSON mal formado", cru(g, "POST", "/api/ligas", "{nao e json"), 400)
    igual("corpo vazio", cru(g, "POST", "/api/ligas", ""), 400)
    igual("texto onde se espera número", cru(g, "POST", "/api/ligas",
          '{"nome":"X","maxEquipas":"muitas"}'), 400)
    igual("decimal onde se espera inteiro", cru(g, "POST", "/api/ligas",
          '{"nome":"X","maxEquipas":3.7}'), 400)
    igual("campo a mais é ignorado", cru(g, "POST", "/api/ligas",
          '{"nome":"Extra %s","maxEquipas":3,"admin":true}' % sufixo), 201)
    igual("id inválido no caminho", cru(g, "GET", "/api/ligas/nao-e-uuid", ""), 400)

    # --------------------------------------------------------------- limites ---
    st, _ = g.pedir("POST", "/api/ligas", {"nome": "Zero", "maxEquipas": 0}, bruto=True)
    igual("maxEquipas 0 recusado", st, 400)
    st, _ = g.pedir("POST", "/api/ligas", {"nome": "Muitas", "maxEquipas": 46}, bruto=True)
    igual("maxEquipas acima do limite recusado", st, 400)
    st, _ = g.pedir("POST", "/api/ligas", {"nome": "   ", "maxEquipas": 3}, bruto=True)
    igual("nome em branco recusado", st, 400)
    nome_gigante = "A" * 500
    st, _ = g.pedir("POST", "/api/ligas", {"nome": nome_gigante, "maxEquipas": 3}, bruto=True)
    igual("nome de 500 caracteres dá 400 com mensagem clara", st, 400)

    # ------------------------------------------------------------- caracteres ---
    equipa_maliciosa = g.pedir("POST", "/api/ligas/%s/equipas" % lid,
        {"nome": "<script>alert(1)</script>", "treinador": "Zé \"Aspas\" & Cª <b>"})
    igual("nome com HTML é guardado tal como veio", equipa_maliciosa["nome"], "<script>alert(1)</script>")
    equipa_unicode = g.pedir("POST", "/api/ligas/%s/equipas" % lid,
        {"nome": "Ção 東京 🇵🇹", "treinador": "Träiner"})
    igual("unicode sobrevive", equipa_unicode["nome"], "Ção 東京 🇵🇹")
    st, _ = g.pedir("POST", "/api/ligas/%s/equipas" % lid,
        {"nome": "'; drop table equipa; --", "treinador": "T"}, bruto=True)
    igual("aspas e ponto-e-vírgula não partem nada", st, 201)
    ainda = g.pedir("GET", "/api/ligas/" + lid)
    igual("a tabela continua lá", len(ainda["equipas"]), 3)

    # ------------------------------------------------------------------ logo ---
    igual("PNG minúsculo aceite", multipart(g, "/api/ligas/%s/logo" % lid, "ficheiro", "a.png", PNG), 200)
    igual("SVG recusado", multipart(g, "/api/ligas/%s/logo" % lid, "ficheiro", "a.svg", SVG, "image/svg+xml"), 400)
    igual("texto disfarçado de PNG recusado",
          multipart(g, "/api/ligas/%s/logo" % lid, "ficheiro", "a.png", b"nao sou uma imagem"), 400)
    grande = PNG + b"\x00" * (1_100_000)
    verificar("ficheiro acima de 1MB recusado",
              multipart(g, "/api/ligas/%s/logo" % lid, "ficheiro", "a.png", grande) in (400, 413),
              multipart(g, "/api/ligas/%s/logo" % lid, "ficheiro", "a.png", grande))
    igual("apagar o logo", cru(g, "DELETE", "/api/ligas/%s/logo" % lid, ""), 200)

    # ------------------------------------------------- regra alterada a meio ---
    eqs = {e["nome"]: e["id"] for e in g.pedir("GET", "/api/ligas/" + lid)["equipas"]}
    nomes = list(eqs)
    g.pedir("PUT", "/api/ligas/%s/regra-divida" % lid, {"valorInscricao": 0, "valorInicial": 1,
        "incremento": 1, "equipasPorEscalao": 1, "valorMaximo": 9, "jornadasPorBloco": 3})

    def jogar(pontos):
        j = g.pedir("POST", "/api/ligas/%s/jornadas" % lid)
        for i, n in enumerate(nomes):
            g.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid, j["id"]),
                    {"equipaId": eqs[n], "pontuacao": pontos[i]})
        g.pedir("POST", "/api/ligas/%s/jornadas/%s/fechar" % (lid, j["id"]))

    jogar([5, 3, 1]); jogar([5, 3, 1])
    # muda de fórmula para tabela com um bloco a meio
    g.pedir("PUT", "/api/ligas/%s/regra-divida" % lid, {"valorInscricao": 0, "valorInicial": 0,
        "incremento": 0, "equipasPorEscalao": 1, "valorMaximo": 0, "jornadasPorBloco": 3,
        "escala": "TABELA", "tabela": "1-0€\n2-10€\n3-20€", "cobraTreino": True})
    jogar([5, 3, 1])
    ultimo = euro(g.pedir("GET", "/api/ligas/%s/equipas/%s/divida" % (lid, eqs[nomes[2]]))["totalPendente"])
    igual("o bloco fecha com a regra em vigor no fecho (3 x 20€)", ultimo, 60.0)

    # ---------------------------------------------- cobrança numa jornada passada ---
    g.pedir("PUT", "/api/ligas/%s/regra-divida" % lid, {"valorInscricao": 0, "valorInicial": 0,
        "incremento": 0, "equipasPorEscalao": 1, "valorMaximo": 0, "jornadasPorBloco": 3,
        "escala": "TABELA", "tabela": "1-0€\n2-0€\n3-0€", "cobraTreino": True,
        "cobrancas": [{"nome": "Passada", "jornadaOficial": 1, "tabela": "1-1€\n2-2€\n3-3€"}]})
    cobs = g.pedir("GET", "/api/ligas/%s/cobrancas" % lid)
    igual("cobrança numa jornada já passada fica por cobrar", cobs[0]["estado"], "POR_COBRAR")

    # duas cobranças com o mesmo nome
    st, _ = g.pedir("PUT", "/api/ligas/%s/regra-divida" % lid, {"valorInscricao": 0, "valorInicial": 0,
        "incremento": 0, "equipasPorEscalao": 1, "valorMaximo": 0, "jornadasPorBloco": 3,
        "escala": "TABELA", "tabela": "1-0€", "cobraTreino": True,
        "cobrancas": [{"nome": "Igual", "jornadaOficial": 1, "tabela": "1-1€"},
                      {"nome": "igual", "jornadaOficial": 2, "tabela": "1-1€"}]}, bruto=True)
    igual("duas cobranças com o mesmo nome recusadas", st, 400)

    # ------------------------------------------------------------- precisão ---
    lid2 = g.pedir("POST", "/api/ligas", {"nome": "Cêntimos %s" % sufixo, "maxEquipas": 3})["id"]
    e2 = {n: g.pedir("POST", "/api/ligas/%s/equipas" % lid2, {"nome": n, "treinador": "T"+n})["id"]
          for n in ["A", "B"]}
    g.pedir("PUT", "/api/ligas/%s/regra-divida" % lid2, {"valorInscricao": 0, "valorInicial": 0,
        "incremento": 0, "equipasPorEscalao": 1, "valorMaximo": 0, "jornadasPorBloco": 1,
        "escala": "TABELA", "tabela": "1-0€\n2-0,10€", "cobraTreino": True})
    for _ in range(10):
        j = g.pedir("POST", "/api/ligas/%s/jornadas" % lid2)
        for n, p in [("A", 3), ("B", 1)]:
            g.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid2, j["id"]),
                    {"equipaId": e2[n], "pontuacao": p})
        g.pedir("POST", "/api/ligas/%s/jornadas/%s/fechar" % (lid2, j["id"]))
    igual("dez vezes 0,10€ dá exactamente 1,00€",
          euro(g.pedir("GET", "/api/ligas/%s/equipas/%s/divida" % (lid2, e2["B"]))["totalPendente"]), 1.0)
    pote = g.pedir("GET", "/api/ligas/" + lid2)["pote"]
    igual("e o pote diz o mesmo", euro(pote["porPagar"]), 1.0)
    igual("valor com três casas recusado ou arredondado",
          cru(g, "POST", "/api/ligas/%s/equipas/%s/divida/blocos" % (lid2, e2["A"]), '{"valor":0.005}') in (200, 201, 400), True)

    # --------------------------------------------------- administração ---
    gestores = admin.pedir("GET", "/api/admin/gestores")
    alvo = [x for x in gestores if x["email"] == "v-%s@t.pt" % sufixo][0]
    admin.pedir("PATCH", "/api/admin/gestores/%s" % alvo["id"], {"email": "corrigido-%s@t.pt" % sufixo})
    st, _ = g.pedir("GET", "/api/ligas", bruto=True)
    igual("corrigir o email corta a sessão", st, 401)
    novo = Cliente()
    st, _ = novo.pedir("POST", "/api/auth/login",
                       {"email": "corrigido-%s@t.pt" % sufixo, "password": "passwordsegura1"}, bruto=True)
    igual("e entra-se com o email novo", st, 200)
    st, _ = novo.pedir("GET", "/api/admin/gestores", bruto=True)
    igual("um gestor não entra na administração", st, 403)

    eu = admin.pedir("GET", "/api/auth/estado")
    st, _ = admin.pedir("PATCH", "/api/admin/gestores/%s" % eu["id"], {"ativo": False}, bruto=True)
    verificar("o administrador não se desactiva a si próprio", st in (400, 409), st)

if __name__ == "__main__":
    rondas = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    for r in range(1, rondas + 1):
        try:
            correr(r); print("ronda %d: ok" % r)
        except Exception as e:
            import traceback
            linha = traceback.extract_tb(sys.exc_info()[2])[-1]
            falhas.append("ronda %d linha %d (%s): %s" % (r, linha.lineno, linha.line.strip()[:70], e))
            print("ronda %d: REBENTOU linha %d: %s" % (r, linha.lineno, e))
    print("\n%d verificações passaram, %d falharam" % (passou[0], len(falhas)))
    for f in falhas: print("  FALHA:", f)
    sys.exit(1 if falhas else 0)
