"""Fluxo completo da aplicação, ponta a ponta, por HTTP.

Cada corrida cria a sua própria liga e as suas próprias contas, para poder
correr muitas vezes seguidas contra a mesma base de dados — que é onde
aparecem os erros de "segunda vez".
"""
import json, sys, uuid, urllib.request, urllib.error, http.cookiejar

API = "http://localhost:8081"
falhas = []
passou = [0]

def sessao():
    jar = http.cookiejar.CookieJar()
    return jar, urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))

class Cliente:
    def __init__(self):
        self.jar, self.opener = sessao()
        self.pedir("GET", "/api/auth/estado", bruto=True)   # semeia o cookie CSRF (401 sem sessão)

    def token(self):
        for c in self.jar:
            if c.name == "XSRF-TOKEN":
                return c.value
        return ""

    def pedir(self, metodo, caminho, corpo=None, bruto=False):
        dados = json.dumps(corpo).encode() if corpo is not None else None
        req = urllib.request.Request(API + caminho, data=dados, method=metodo)
        req.add_header("Content-Type", "application/json")
        if self.token():
            req.add_header("X-XSRF-TOKEN", self.token())
        try:
            with self.opener.open(req) as r:
                texto = r.read().decode()
                corpo_lido = json.loads(texto) if texto.strip() else None
                return (r.status, corpo_lido) if bruto else corpo_lido
        except urllib.error.HTTPError as e:
            texto = e.read().decode()
            corpo_lido = json.loads(texto) if texto.strip() else None
            if bruto:
                return e.code, corpo_lido
            raise AssertionError("%s %s -> %s %s" % (metodo, caminho, e.code, texto[:200]))

def verificar(nome, condicao, detalhe=""):
    if condicao:
        passou[0] += 1
    else:
        falhas.append("%s %s" % (nome, ("— " + str(detalhe)) if detalhe else ""))

def igual(nome, obtido, esperado):
    verificar(nome, obtido == esperado, "esperava %r, veio %r" % (esperado, obtido))

def euro(valor):
    return round(float(valor), 2)

def correr(ronda):
    sufixo = uuid.uuid4().hex[:8]

    # ---------------------------------------------------------------- admin ---
    admin = Cliente()
    admin.pedir("POST", "/api/auth/login", {"email": "admin@teste.pt", "password": "passwordsegura1"})
    estado = admin.pedir("GET", "/api/auth/estado")
    verificar("admin entra", estado and estado["admin"])

    # convite de gestor -> conta nova
    convite = admin.pedir("POST", "/api/admin/convites", {"nota": "e2e " + sufixo, "diasValidade": 7})
    verificar("convite de gestor traz código", bool(convite.get("codigo")))

    gestor = Cliente()
    email_gestor = "gestor-%s@teste.pt" % sufixo
    gestor.pedir("POST", "/api/auth/registo",
                 {"codigo": convite["codigo"], "nome": "Gestor " + sufixo,
                  "email": email_gestor, "password": "passwordsegura1"})
    eu = gestor.pedir("GET", "/api/auth/estado")
    verificar("gestor novo pode criar ligas", eu["podeCriarLigas"])

    st, _ = Cliente().pedir("POST", "/api/auth/registo",
        {"codigo": convite["codigo"], "nome": "X", "email": "x-%s@t.pt" % sufixo,
         "password": "passwordsegura1"}, bruto=True)
    igual("convite de gestor serve uma vez", st, 403)

    # ----------------------------------------------------------------- liga ---
    liga = gestor.pedir("POST", "/api/ligas", {"nome": "Liga %s" % sufixo, "maxEquipas": 5})
    lid = liga["id"]
    igual("liga nasce activa", liga["estado"], "ATIVA")
    gestor.pedir("PATCH", "/api/ligas/" + lid, {"pontosTreinoContam": False})

    # regra por tabela, treino sem cobrança, com Inverno na oficial 2
    semanal = "1-0€\n2-0,10€\n3-0,30€\n4-0,50€"
    inverno = "1-0€\n2-0,50€\n3-1€\n4-1,50€"
    regra = gestor.pedir("PUT", "/api/ligas/%s/regra-divida" % lid, {
        "valorInscricao": 2, "valorInicial": 0, "incremento": 0, "equipasPorEscalao": 1,
        "valorMaximo": 0, "jornadasPorBloco": 1, "escala": "TABELA", "tabela": semanal,
        "cobraTreino": False,
        "cobrancas": [{"nome": "Inverno", "jornadaOficial": 2, "tabela": inverno}]})
    igual("regra guardada por tabela", regra["escala"], "TABELA")
    igual("regra com uma cobrança", len(regra["cobrancas"]), 1)

    # guardar duas vezes seguidas (o bug que apanhei antes)
    st, _ = gestor.pedir("PUT", "/api/ligas/%s/regra-divida" % lid, {
        "valorInscricao": 2, "valorInicial": 0, "incremento": 0, "equipasPorEscalao": 1,
        "valorMaximo": 0, "jornadasPorBloco": 1, "escala": "TABELA", "tabela": semanal,
        "cobraTreino": False,
        "cobrancas": [{"nome": "Inverno", "jornadaOficial": 2, "tabela": inverno}]}, bruto=True)
    igual("guardar a regra duas vezes", st, 200)

    # tabela com salto -> 400
    st, corpo = gestor.pedir("PUT", "/api/ligas/%s/regra-divida" % lid, {
        "valorInscricao": 0, "valorInicial": 0, "incremento": 0, "equipasPorEscalao": 1,
        "valorMaximo": 0, "jornadasPorBloco": 1, "escala": "TABELA", "tabela": "1-0€\n3-1€"}, bruto=True)
    igual("tabela com salto recusada", st, 400)

    # --------------------------------------------------------------- equipas ---
    equipas = {}
    for nome, email in [("Alfa", "alfa-%s@t.pt" % sufixo), ("Bravo", None), ("Charlie", None)]:
        e = gestor.pedir("POST", "/api/ligas/%s/equipas" % lid,
                         {"nome": nome, "treinador": "T" + nome, "treinadorEmail": email})
        equipas[nome] = e["id"]
    igual("três equipas", len(equipas), 3)

    # nome repetido -> 409
    st, _ = gestor.pedir("POST", "/api/ligas/%s/equipas" % lid,
                         {"nome": "Alfa", "treinador": "Outro"}, bruto=True)
    igual("equipa com nome repetido recusada", st, 409)

    # email inválido -> 400
    st, _ = gestor.pedir("POST", "/api/ligas/%s/equipas" % lid,
                         {"nome": "Delta", "treinador": "TDelta", "treinadorEmail": "nao-e-email"}, bruto=True)
    igual("email de treinador inválido recusado", st, 400)

    # a inscrição foi cobrada às três
    dividas = gestor.pedir("GET", "/api/ligas/%s/dividas" % lid)
    igual("inscrição cobrada a cada equipa", len(dividas), 3)
    igual("inscrição de 2€", euro(dividas[0]["totalPendente"]), 2.0)

    # ------------------------------------------------------------- convites ---
    convite_t = gestor.pedir("POST", "/api/ligas/%s/equipas/%s/convites-treinador" % (lid, equipas["Alfa"]),
                             {"diasValidade": None})
    verificar("convite de treinador traz link", bool(convite_t.get("link")))
    igual("com email tenta enviar", convite_t["envio"], "FALHOU")   # sem chave Resend

    st, repetido = gestor.pedir("POST", "/api/ligas/%s/equipas/%s/convites-treinador" % (lid, equipas["Alfa"]),
                                {"diasValidade": None}, bruto=True)
    igual("emitir outra vez devolve o mesmo", repetido["codigo"], convite_t["codigo"])
    igual("e responde 200 em vez de 201", st, 200)

    publico = Cliente().pedir("GET", "/api/convites-treinador/" + convite_t["codigo"])
    igual("leitura pública diz a equipa", publico["equipaNome"], "Alfa")
    verificar("leitura pública não traz contactos", "email" not in json.dumps(publico).lower())

    treinador = Cliente()
    email_treinador = "treinador-%s@t.pt" % sufixo
    treinador.pedir("POST", "/api/auth/registo-treinador",
                    {"codigo": convite_t["codigo"], "nome": "TAlfa",
                     "email": email_treinador, "password": "passwordsegura1"})
    conta_t = treinador.pedir("GET", "/api/auth/estado")
    verificar("conta de treinador não cria ligas", not conta_t["podeCriarLigas"])

    st, _ = Cliente().pedir("GET", "/api/convites-treinador/" + convite_t["codigo"], bruto=True)
    igual("convite gasto deixa de ser lido", st, 403)

    detalhe = gestor.pedir("GET", "/api/ligas/" + lid)
    alfa = [e for e in detalhe["equipas"] if e["nome"] == "Alfa"][0]
    igual("estado da conta na tabela", alfa["conviteEstado"], "LIGADA")

    # convite em massa para as que faltam
    lote = gestor.pedir("POST", "/api/ligas/%s/convites-treinador" % lid)
    igual("lote convida as duas em falta", lote["convidadas"], 2)
    estados = sorted(l["estado"] for l in lote["equipas"])
    igual("lote com JA_TEM_CONTA e SEM_EMAIL", estados, ["JA_TEM_CONTA", "SEM_EMAIL", "SEM_EMAIL"])

    # ligar um convite a uma conta que já existe
    lote2 = gestor.pedir("POST", "/api/ligas/%s/convites-treinador" % lid)
    link_bravo = [l for l in lote2["equipas"] if l["equipa"] == "Bravo"][0]["link"]
    codigo_bravo = link_bravo.split("c=")[1]
    st, _ = treinador.pedir("POST", "/api/auth/treinador/ligar", {"codigo": codigo_bravo}, bruto=True)
    igual("mesma conta liga uma segunda equipa", st, 204)

    # -------------------------------------------------------------- jornadas ---
    desistentes = set()

    def jogar(pa, pb, pc, esperar=200):
        j = gestor.pedir("POST", "/api/ligas/%s/jornadas" % lid)
        for nome, pontos in [("Alfa", pa), ("Bravo", pb), ("Charlie", pc)]:
            if nome in desistentes:
                continue
            gestor.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid, j["id"]),
                         {"equipaId": equipas[nome], "pontuacao": pontos})
        st, fechada = gestor.pedir("POST", "/api/ligas/%s/jornadas/%s/fechar" % (lid, j["id"]), bruto=True)
        return fechada

    for _ in range(5):
        jogar(5, 3, 1)      # treino
    dividas = gestor.pedir("GET", "/api/ligas/%s/dividas" % lid)
    igual("treino não cobra", euro(sum(d["totalPendente"] for d in dividas)), 6.0)  # só as 3 inscrições

    classificacao = gestor.pedir("GET", "/api/ligas/%s/classificacao" % lid)
    igual("pontos do treino não contam", classificacao[0]["pontos"], 0)

    jogar(5, 3, 1)          # oficial 1
    dividas = {d["equipaNome"]: euro(d["totalPendente"]) for d in gestor.pedir("GET", "/api/ligas/%s/dividas" % lid)}
    igual("semanal: 1º não paga", dividas["Alfa"], 2.0)
    igual("semanal: 2º paga 0,10", dividas["Bravo"], 2.10)
    igual("semanal: 3º paga 0,30", dividas["Charlie"], 2.30)

    # jornada empatada não fecha
    j = gestor.pedir("POST", "/api/ligas/%s/jornadas" % lid)
    for nome, pontos in [("Alfa", 2), ("Bravo", 2), ("Charlie", 1)]:
        gestor.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid, j["id"]),
                     {"equipaId": equipas[nome], "pontuacao": pontos})
    fechada = gestor.pedir("POST", "/api/ligas/%s/jornadas/%s/fechar" % (lid, j["id"]))
    igual("jornada empatada fica em desempate", fechada["estado"], "DESEMPATE")

    st, _ = gestor.pedir("POST", "/api/ligas/%s/jornadas/%s/fechar" % (lid, j["id"]), bruto=True)
    igual("e não fecha à força", st, 409)

    gestor.pedir("POST", "/api/ligas/%s/jornadas/%s/desempate" % (lid, j["id"]),
                 {"ordem": [equipas["Bravo"], equipas["Alfa"]]})
    detalhe = gestor.pedir("GET", "/api/ligas/" + lid)
    oficial2 = [x for x in detalhe["jornadas"] if x["tipo"] == "OFICIAL" and x["numero"] == 2][0]
    igual("desempate fecha a jornada", oficial2["estado"], "FECHADA")

    # a cobrança de época caiu na oficial 2
    cobrancas = gestor.pedir("GET", "/api/ligas/%s/cobrancas" % lid)
    igual("Inverno cobrado na oficial 2", cobrancas[0]["estado"], "COBRADA")
    blocos = [b for d in gestor.pedir("GET", "/api/ligas/%s/dividas" % lid) for b in d["blocos"]]
    verificar("bloco com o nome Inverno", any(b["nome"] == "Inverno" for b in blocos))

    # ------------------------------------------------------------ pagamentos ---
    divida_charlie = [d for d in gestor.pedir("GET", "/api/ligas/%s/dividas" % lid)
                      if d["equipaNome"] == "Charlie"][0]
    pendente = [b for b in divida_charlie["blocos"] if b["estado"] == "PENDENTE"][0]
    gestor.pedir("POST", "/api/ligas/%s/equipas/%s/divida/blocos/%s/pagar" % (lid, equipas["Charlie"], pendente["id"]))
    st, _ = gestor.pedir("POST", "/api/ligas/%s/equipas/%s/divida/blocos/%s/pagar" % (lid, equipas["Charlie"], pendente["id"]), bruto=True)
    igual("pagar duas vezes o mesmo bloco", st, 409)

    gestor.pedir("POST", "/api/ligas/%s/equipas/%s/divida/pagar" % (lid, equipas["Charlie"]))
    charlie = gestor.pedir("GET", "/api/ligas/%s/equipas/%s/divida" % (lid, equipas["Charlie"]))
    igual("pagar tudo zera a dívida", euro(charlie["totalPendente"]), 0.0)
    igual("e o estado passa a resolvida", charlie["estado"], "RESOLVIDA")
    pendentes = gestor.pedir("GET", "/api/ligas/%s/dividas" % lid)
    verificar("quem pagou tudo sai da lista de pendentes",
              all(d["equipaNome"] != "Charlie" for d in pendentes))

    # --------------------------------------------------------- desistência ---
    gestor.pedir("POST", "/api/ligas/%s/equipas/%s/desistencia" % (lid, equipas["Charlie"]))
    desistentes.add("Charlie")

    def divida_de(nome):
        return euro(gestor.pedir("GET", "/api/ligas/%s/equipas/%s/divida" % (lid, equipas[nome]))["totalPendente"])

    antes = {n: divida_de(n) for n in equipas}
    jornada = gestor.pedir("POST", "/api/ligas/%s/jornadas" % lid)
    st, _ = gestor.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid, jornada["id"]),
                         {"equipaId": equipas["Charlie"], "pontuacao": 1}, bruto=True)
    igual("desistente não recebe resultados", st, 409)
    for nome, pontos in [("Alfa", 3), ("Bravo", 2)]:
        gestor.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid, jornada["id"]),
                     {"equipaId": equipas[nome], "pontuacao": pontos})
    gestor.pedir("POST", "/api/ligas/%s/jornadas/%s/fechar" % (lid, jornada["id"]))
    depois = {n: divida_de(n) for n in equipas}
    igual("desistente deixa de ser cobrada", depois["Charlie"], antes["Charlie"])
    igual("e as outras encurtam a tabela", euro(depois["Bravo"] - antes["Bravo"]), 0.10)

    # ---------------------------------------------------- vistas do treinador ---
    minhas = treinador.pedir("GET", "/api/minhas-ligas")
    igual("treinador vê a liga onde treina", len(minhas), 1)
    detalhe_t = treinador.pedir("GET", "/api/minhas-ligas/" + lid)
    verificar("treinador não vê estado de convites",
              all(e["conviteEstado"] is None for e in detalhe_t["equipas"]))
    minhas_dividas = treinador.pedir("GET", "/api/minhas-dividas")
    igual("treinador vê as duas equipas que treina", len(minhas_dividas["equipas"]), 2)

    # ------------------------------------------------------------ autorização ---
    outro = Cliente()
    convite2 = admin.pedir("POST", "/api/admin/convites", {"nota": "outro " + sufixo, "diasValidade": 7})
    outro.pedir("POST", "/api/auth/registo",
                {"codigo": convite2["codigo"], "nome": "Outro", "email": "outro-%s@t.pt" % sufixo,
                 "password": "passwordsegura1"})
    st, _ = outro.pedir("GET", "/api/ligas/" + lid, bruto=True)
    igual("liga de outro gestor não existe", st, 404)
    st, _ = outro.pedir("POST", "/api/ligas/%s/convites-treinador" % lid, bruto=True)
    igual("nem se convida nela", st, 404)
    st, _ = treinador.pedir("GET", "/api/ligas/" + lid, bruto=True)
    igual("treinador não entra pela porta do gestor", st, 404)
    st, _ = Cliente().pedir("GET", "/api/ligas/" + lid, bruto=True)
    igual("sem sessão não se vê nada", st, 401)

    # --------------------------------------------------------------- conta ---
    treinador.pedir("POST", "/api/auth/password",
                    {"atual": "passwordsegura1", "nova": "outrapasswordsegura"})
    st, _ = treinador.pedir("GET", "/api/auth/estado", bruto=True)
    igual("mudar a password corta a sessão", st, 401)
    novo = Cliente()
    st, _ = novo.pedir("POST", "/api/auth/login",
                       {"email": email_treinador, "password": "outrapasswordsegura"}, bruto=True)
    igual("e entra-se com a nova", st, 200)

    # ------------------------------------------------------------ terminar ---
    gestor.pedir("POST", "/api/ligas/%s/terminar" % lid)
    st, _ = gestor.pedir("POST", "/api/ligas/%s/equipas" % lid,
                         {"nome": "Tarde", "treinador": "T"}, bruto=True)
    igual("liga terminada não recebe equipas", st, 409)
    st, _ = gestor.pedir("POST", "/api/ligas/%s/jornadas" % lid, bruto=True)
    igual("nem abre jornadas", st, 409)

if __name__ == "__main__":
    rondas = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    for r in range(1, rondas + 1):
        try:
            correr(r)
            print("ronda %d: ok" % r)
        except Exception as e:
            import traceback
            linha = traceback.extract_tb(sys.exc_info()[2])[-1]
            falhas.append("ronda %d rebentou na linha %d (%s): %s" % (r, linha.lineno, linha.line.strip()[:80], e))
            print("ronda %d: REBENTOU linha %d: %s | %s" % (r, linha.lineno, linha.line.strip()[:90], e))
    print("\n%d verificações passaram, %d falharam" % (passou[0], len(falhas)))
    for f in falhas:
        print("  FALHA:", f)
    sys.exit(1 if falhas else 0)
