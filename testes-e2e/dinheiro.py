"""Concorrência sobre dinheiro, e o que sobrevive a um reinício."""
import json, sys, uuid, threading, urllib.request, urllib.error, http.cookiejar

exec(open("/tmp/e2e.py").read().split('def correr(')[0])

def admin_cliente():
    c = Cliente()
    c.pedir("POST", "/api/auth/login", {"email": "admin@teste.pt", "password": "passwordsegura1"})
    return c

def montar(sufixo, jornadas_por_bloco=1):
    admin = admin_cliente()
    convite = admin.pedir("POST", "/api/admin/convites", {"nota": sufixo, "diasValidade": 7})
    gestor = Cliente()
    gestor.pedir("POST", "/api/auth/registo", {"codigo": convite["codigo"], "nome": "G",
                 "email": "g-%s@t.pt" % sufixo, "password": "passwordsegura1"})
    lid = gestor.pedir("POST", "/api/ligas", {"nome": "Dinheiro %s" % sufixo, "maxEquipas": 5})["id"]
    eq = {n: gestor.pedir("POST", "/api/ligas/%s/equipas" % lid,
                          {"nome": n, "treinador": "T" + n})["id"] for n in ["Um", "Dois", "Tres"]}
    gestor.pedir("PUT", "/api/ligas/%s/regra-divida" % lid, {
        "valorInscricao": 0, "valorInicial": 0, "incremento": 0, "equipasPorEscalao": 1,
        "valorMaximo": 0, "jornadasPorBloco": jornadas_por_bloco, "escala": "TABELA",
        "tabela": "1-0€\n2-1€\n3-2€", "cobraTreino": True})
    return gestor, lid, eq

def total(gestor, lid, equipa):
    return euro(gestor.pedir("GET", "/api/ligas/%s/equipas/%s/divida" % (lid, equipa))["totalPendente"])

def correr(ronda):
    sufixo = uuid.uuid4().hex[:8]
    gestor, lid, eq = montar(sufixo)

    # --- fechar a mesma jornada em paralelo: uma cobra, as outras dão 409 ------
    j = gestor.pedir("POST", "/api/ligas/%s/jornadas" % lid)
    for nome, pontos in [("Um", 3), ("Dois", 2), ("Tres", 1)]:
        gestor.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid, j["id"]),
                     {"equipaId": eq[nome], "pontuacao": pontos})

    codigos = []
    def fechar():
        try:
            st, _ = gestor.pedir("POST", "/api/ligas/%s/jornadas/%s/fechar" % (lid, j["id"]), bruto=True)
            codigos.append(st)
        except Exception as e:
            codigos.append(str(e))
    fios = [threading.Thread(target=fechar) for _ in range(5)]
    [f.start() for f in fios]; [f.join() for f in fios]

    verificar("fechar em paralelo: nenhum 500", all(c in (200, 409) for c in codigos), codigos)
    igual("um só fecho vingou", sorted(codigos).count(200), 1)
    igual("e a 3ª foi cobrada uma só vez", total(gestor, lid, eq["Tres"]), 2.0)

    # --- pagar o mesmo bloco em paralelo -------------------------------------
    divida = gestor.pedir("GET", "/api/ligas/%s/equipas/%s/divida" % (lid, eq["Tres"]))
    bloco = [b for b in divida["blocos"] if b["estado"] == "PENDENTE"][0]
    pagamentos = []
    def pagar():
        try:
            st, _ = gestor.pedir("POST", "/api/ligas/%s/equipas/%s/divida/blocos/%s/pagar"
                                 % (lid, eq["Tres"], bloco["id"]), bruto=True)
            pagamentos.append(st)
        except Exception as e:
            pagamentos.append(str(e))
    fios = [threading.Thread(target=pagar) for _ in range(5)]
    [f.start() for f in fios]; [f.join() for f in fios]
    verificar("pagar em paralelo: nenhum 500", all(c in (200, 409) for c in pagamentos), pagamentos)
    igual("o bloco fica pago uma vez", total(gestor, lid, eq["Tres"]), 0.0)

    # --- lançar blocos à mão em paralelo (números de bloco) -------------------
    lancamentos = []
    def lancar():
        try:
            st, _ = gestor.pedir("POST", "/api/ligas/%s/equipas/%s/divida/blocos" % (lid, eq["Um"]),
                                 {"valor": 1.0}, bruto=True)
            lancamentos.append(st)
        except Exception as e:
            lancamentos.append(str(e))
    fios = [threading.Thread(target=lancar) for _ in range(6)]
    [f.start() for f in fios]; [f.join() for f in fios]
    verificar("lançar blocos em paralelo: nenhum 500", all(c in (200, 201, 409) for c in lancamentos), lancamentos)
    esperado = euro(sum(1.0 for c in lancamentos if c in (200, 201)))
    igual("o total bate com os que passaram", total(gestor, lid, eq["Um"]), esperado)

    # --- bloco de período com mais de uma jornada ----------------------------
    gestor2, lid2, eq2 = montar(uuid.uuid4().hex[:8], jornadas_por_bloco=3)
    for i in range(2):
        j = gestor2.pedir("POST", "/api/ligas/%s/jornadas" % lid2)
        for nome, pontos in [("Um", 3), ("Dois", 2), ("Tres", 1)]:
            gestor2.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid2, j["id"]),
                          {"equipaId": eq2[nome], "pontuacao": pontos})
        gestor2.pedir("POST", "/api/ligas/%s/jornadas/%s/fechar" % (lid2, j["id"]))
    igual("duas jornadas ainda não fecham o bloco", total(gestor2, lid2, eq2["Tres"]), 0.0)

    j = gestor2.pedir("POST", "/api/ligas/%s/jornadas" % lid2)
    for nome, pontos in [("Um", 3), ("Dois", 2), ("Tres", 1)]:
        gestor2.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid2, j["id"]),
                      {"equipaId": eq2[nome], "pontuacao": pontos})
    gestor2.pedir("POST", "/api/ligas/%s/jornadas/%s/fechar" % (lid2, j["id"]))
    igual("a terceira fecha e cobra as três", total(gestor2, lid2, eq2["Tres"]), 6.0)

if __name__ == "__main__":
    rondas = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    for r in range(1, rondas + 1):
        try:
            correr(r); print("ronda %d: ok" % r)
        except Exception as e:
            import traceback
            linha = traceback.extract_tb(sys.exc_info()[2])[-1]
            falhas.append("ronda %d linha %d: %s" % (r, linha.lineno, e))
            print("ronda %d: REBENTOU linha %d: %s" % (r, linha.lineno, e))
    print("\n%d verificações passaram, %d falharam" % (passou[0], len(falhas)))
    for f in falhas: print("  FALHA:", f)
    sys.exit(1 if falhas else 0)
