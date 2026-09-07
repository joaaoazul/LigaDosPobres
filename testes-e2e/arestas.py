"""As arestas: concorrência, limites, autorização, e o que acontece depois de reiniciar."""
import json, sys, uuid, threading, urllib.request, urllib.error, http.cookiejar

exec(open("/tmp/e2e.py").read().split('def correr(')[0])   # reaproveita Cliente/verificar/igual

def admin_cliente():
    c = Cliente()
    c.pedir("POST", "/api/auth/login", {"email": "admin@teste.pt", "password": "passwordsegura1"})
    return c

def novo_gestor(admin, sufixo):
    convite = admin.pedir("POST", "/api/admin/convites", {"nota": sufixo, "diasValidade": 7})
    c = Cliente()
    email = "g-%s@t.pt" % sufixo
    c.pedir("POST", "/api/auth/registo", {"codigo": convite["codigo"], "nome": "G" + sufixo,
                                          "email": email, "password": "passwordsegura1"})
    return c, email

def correr(ronda):
    sufixo = uuid.uuid4().hex[:8]
    admin = admin_cliente()
    gestor, email_gestor = novo_gestor(admin, sufixo)
    lid = gestor.pedir("POST", "/api/ligas", {"nome": "Arestas %s" % sufixo, "maxEquipas": 3})["id"]
    equipa = gestor.pedir("POST", "/api/ligas/%s/equipas" % lid,
                          {"nome": "Alfa", "treinador": "TAlfa"})["id"]

    # --- liga cheia -----------------------------------------------------------
    gestor.pedir("POST", "/api/ligas/%s/equipas" % lid, {"nome": "Bravo", "treinador": "TB"})
    gestor.pedir("POST", "/api/ligas/%s/equipas" % lid, {"nome": "Charlie", "treinador": "TC"})
    st, _ = gestor.pedir("POST", "/api/ligas/%s/equipas" % lid, {"nome": "Delta", "treinador": "TD"}, bruto=True)
    igual("liga cheia recusa mais uma", st, 409)

    # --- pontuações inválidas -------------------------------------------------
    j = gestor.pedir("POST", "/api/ligas/%s/jornadas" % lid)
    st, _ = gestor.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid, j["id"]),
                         {"equipaId": equipa, "pontuacao": -1}, bruto=True)
    igual("pontuação negativa recusada", st, 400)
    st, _ = gestor.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid, j["id"]),
                         {"equipaId": equipa, "pontuacao": 1.5}, bruto=True)
    igual("pontuação decimal recusada", st, 400)
    st, _ = gestor.pedir("POST", "/api/ligas/%s/jornadas/%s/fechar" % (lid, j["id"]), bruto=True)
    igual("jornada sem resultados não fecha", st, 409)

    # --- convites: revogar, expirar, duas vezes -------------------------------
    c1 = gestor.pedir("POST", "/api/ligas/%s/equipas/%s/convites-treinador" % (lid, equipa),
                      {"diasValidade": 1})
    gestor.pedir("DELETE", "/api/ligas/%s/equipas/%s/convites-treinador/%s" % (lid, equipa, c1["id"]))
    st, _ = Cliente().pedir("GET", "/api/convites-treinador/" + c1["codigo"], bruto=True)
    igual("convite revogado deixa de servir", st, 403)
    st, _ = gestor.pedir("DELETE", "/api/ligas/%s/equipas/%s/convites-treinador/%s" % (lid, equipa, c1["id"]), bruto=True)
    igual("revogar duas vezes", st, 409)

    c2 = gestor.pedir("POST", "/api/ligas/%s/equipas/%s/convites-treinador" % (lid, equipa), {"diasValidade": None})
    verificar("depois de revogar emite um novo", c2["codigo"] != c1["codigo"])
    st, _ = gestor.pedir("POST", "/api/ligas/%s/equipas/%s/convites-treinador" % (lid, equipa),
                         {"diasValidade": 400}, bruto=True)
    igual("validade fora do intervalo recusada", st, 400)

    # --- emitir em paralelo (a corrida do índice único) -----------------------
    resultados = []
    def emitir():
        try:
            st, _ = gestor.pedir("POST", "/api/ligas/%s/equipas/%s/convites-treinador"
                                 % (lid, gestor_equipa_paralela), {"diasValidade": None}, bruto=True)
            resultados.append(st)
        except Exception as e:
            resultados.append(str(e))
    gestor_equipa_paralela = [e["id"] for e in gestor.pedir("GET", "/api/ligas/" + lid)["equipas"]
                              if e["nome"] == "Bravo"][0]
    fios = [threading.Thread(target=emitir) for _ in range(6)]
    [f.start() for f in fios]; [f.join() for f in fios]
    verificar("emitir em paralelo nunca dá 500", all(r in (200, 201, 409) for r in resultados), resultados)
    codigos = gestor.pedir("GET", "/api/ligas/" + lid)
    bravo = [e for e in codigos["equipas"] if e["nome"] == "Bravo"][0]
    igual("e fica um só convite vivo", bravo["conviteEstado"], "PENDENTE")

    # --- registo com convite: erros ------------------------------------------
    st, _ = Cliente().pedir("POST", "/api/auth/registo-treinador",
                            {"codigo": c2["codigo"], "nome": "X", "email": "x-%s@t.pt" % sufixo,
                             "password": "curta"}, bruto=True)
    igual("password curta recusada", st, 400)
    st, _ = Cliente().pedir("POST", "/api/auth/registo-treinador",
                            {"codigo": c2["codigo"], "nome": "X", "email": email_gestor,
                             "password": "passwordsegura1"}, bruto=True)
    igual("email já usado recusado", st, 409)
    st, _ = Cliente().pedir("POST", "/api/auth/registo-treinador",
                            {"codigo": "inventado", "nome": "X", "email": "y-%s@t.pt" % sufixo,
                             "password": "passwordsegura1"}, bruto=True)
    igual("código inventado recusado", st, 403)

    treinador = Cliente()
    treinador.pedir("POST", "/api/auth/registo-treinador",
                    {"codigo": c2["codigo"], "nome": "TAlfa", "email": "t-%s@t.pt" % sufixo,
                     "password": "passwordsegura1"})
    st, _ = gestor.pedir("POST", "/api/ligas/%s/equipas/%s/convites-treinador" % (lid, equipa),
                         {"diasValidade": None}, bruto=True)
    igual("não se convida quem já tem conta", st, 409)

    # desligar a conta e voltar a convidar
    gestor.pedir("DELETE", "/api/ligas/%s/equipas/%s/treinador/conta" % (lid, equipa))
    st, _ = gestor.pedir("POST", "/api/ligas/%s/equipas/%s/convites-treinador" % (lid, equipa),
                         {"diasValidade": None}, bruto=True)
    igual("depois de desligar volta a poder convidar", st, 201)
    st, _ = gestor.pedir("DELETE", "/api/ligas/%s/equipas/%s/treinador/conta" % (lid, equipa), bruto=True)
    igual("desligar sem conta ligada", st, 409)

    # --- cobrança de época com empate, por HTTP -------------------------------
    lid2 = gestor.pedir("POST", "/api/ligas", {"nome": "Empate %s" % sufixo, "maxEquipas": 5})["id"]
    gestor.pedir("PATCH", "/api/ligas/" + lid2, {"pontosTreinoContam": False})
    eq = {n: gestor.pedir("POST", "/api/ligas/%s/equipas" % lid2,
                          {"nome": n, "treinador": "T" + n})["id"] for n in ["Um", "Dois", "Tres"]}
    gestor.pedir("PUT", "/api/ligas/%s/regra-divida" % lid2, {
        "valorInscricao": 0, "valorInicial": 0, "incremento": 0, "equipasPorEscalao": 1,
        "valorMaximo": 0, "jornadasPorBloco": 1, "escala": "TABELA", "tabela": "1-0€\n2-0€\n3-0€",
        "cobraTreino": False,
        "cobrancas": [{"nome": "Inverno", "jornadaOficial": 2, "tabela": "1-0€\n2-0,50€\n3-1€"}]})

    def jogar2(a, b, c):
        j = gestor.pedir("POST", "/api/ligas/%s/jornadas" % lid2)
        for nome, pontos in [("Um", a), ("Dois", b), ("Tres", c)]:
            gestor.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid2, j["id"]),
                         {"equipaId": eq[nome], "pontuacao": pontos})
        gestor.pedir("POST", "/api/ligas/%s/jornadas/%s/fechar" % (lid2, j["id"]))

    for _ in range(5):
        jogar2(5, 3, 1)
    jogar2(5, 3, 1)      # oficial 1: 5/3/1
    jogar2(1, 3, 2)      # oficial 2: 6/6/3 -> empate

    cob = gestor.pedir("GET", "/api/ligas/%s/cobrancas" % lid2)[0]
    igual("empate trava a cobrança", cob["estado"], "A_ESPERA_DE_DESEMPATE")
    igual("e diz quem está empatado", len(cob["empates"][0]["equipas"]), 2)

    st, _ = gestor.pedir("POST", "/api/ligas/%s/cobrancas/%s/desempate" % (lid2, cob["id"]),
                         {"ordem": [eq["Um"]]}, bruto=True)
    igual("ordem incompleta recusada", st, 400)
    st, _ = gestor.pedir("POST", "/api/ligas/%s/cobrancas/%s/desempate" % (lid2, cob["id"]),
                         {"ordem": [eq["Um"], eq["Dois"], eq["Tres"]]}, bruto=True)
    igual("ordem com quem não está empatado recusada", st, 400)

    gestor.pedir("POST", "/api/ligas/%s/cobrancas/%s/desempate" % (lid2, cob["id"]),
                 {"ordem": [eq["Dois"], eq["Um"]]})
    d_um = euro(gestor.pedir("GET", "/api/ligas/%s/equipas/%s/divida" % (lid2, eq["Um"]))["totalPendente"])
    d_dois = euro(gestor.pedir("GET", "/api/ligas/%s/equipas/%s/divida" % (lid2, eq["Dois"]))["totalPendente"])
    igual("quem o gestor pôs à frente paga 0", d_dois, 0.0)
    igual("e o outro paga o degrau seguinte", d_um, 0.50)
    st, _ = gestor.pedir("POST", "/api/ligas/%s/cobrancas/%s/desempate" % (lid2, cob["id"]),
                         {"ordem": [eq["Dois"], eq["Um"]]}, bruto=True)
    igual("desempatar outra vez", st, 409)

    # --- posição para lá do fim da tabela ------------------------------------
    gestor.pedir("PUT", "/api/ligas/%s/regra-divida" % lid2, {
        "valorInscricao": 0, "valorInicial": 0, "incremento": 0, "equipasPorEscalao": 1,
        "valorMaximo": 0, "jornadasPorBloco": 1, "escala": "TABELA", "tabela": "1-0€\n2-1€",
        "cobraTreino": False})
    antes = euro(gestor.pedir("GET", "/api/ligas/%s/equipas/%s/divida" % (lid2, eq["Tres"]))["totalPendente"])
    jogar2(5, 3, 1)
    depois = euro(gestor.pedir("GET", "/api/ligas/%s/equipas/%s/divida" % (lid2, eq["Tres"]))["totalPendente"])
    igual("3ª equipa com tabela de 2 linhas paga a última", euro(depois - antes), 1.0)

    # --- administração --------------------------------------------------------
    gestores = admin.pedir("GET", "/api/admin/gestores")
    alvo = [g for g in gestores if g["email"] == email_gestor][0]
    admin.pedir("PATCH", "/api/admin/gestores/" + alvo["id"], {"podeCriarLigas": False})
    st, _ = gestor.pedir("POST", "/api/ligas", {"nome": "Nao", "maxEquipas": 3}, bruto=True)
    igual("sem permissão não cria ligas", st, 403)
    admin.pedir("PATCH", "/api/admin/gestores/" + alvo["id"], {"ativo": False})
    st, _ = gestor.pedir("GET", "/api/ligas", bruto=True)
    igual("conta desactivada é barrada no pedido seguinte", st, 401)
    admin.pedir("PATCH", "/api/admin/gestores/" + alvo["id"], {"ativo": True})

    # --- recuperação de password ---------------------------------------------
    st, _ = Cliente().pedir("POST", "/api/auth/recuperar", {"email": "nao-existe-%s@t.pt" % sufixo}, bruto=True)
    igual("recuperar responde igual para email que não existe", st, 204)
    st, _ = Cliente().pedir("POST", "/api/auth/recuperar", {"email": email_gestor}, bruto=True)
    igual("e igual para um que existe", st, 204)

if __name__ == "__main__":
    rondas = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    for r in range(1, rondas + 1):
        try:
            correr(r)
            print("ronda %d: ok" % r)
        except Exception as e:
            import traceback
            linha = traceback.extract_tb(sys.exc_info()[2])[-1]
            falhas.append("ronda %d linha %d (%s): %s" % (r, linha.lineno, linha.line.strip()[:70], e))
            print("ronda %d: REBENTOU linha %d: %s" % (r, linha.lineno, e))
    print("\n%d verificações passaram, %d falharam" % (passou[0], len(falhas)))
    for f in falhas:
        print("  FALHA:", f)
    sys.exit(1 if falhas else 0)
