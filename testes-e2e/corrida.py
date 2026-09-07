"""Martela o fecho da mesma jornada e conta quantos vingam."""
import sys, uuid, threading, json, urllib.request, urllib.error, http.cookiejar
PORTA = sys.argv[1]
VEZES = int(sys.argv[2]) if len(sys.argv) > 2 else 10
src = open("/tmp/e2e.py").read().split('def correr(')[0].replace(
    'API = "http://localhost:8081"', 'API = "http://localhost:%s"' % PORTA)
exec(src)

def uma_vez(i, formula):
    sufixo = uuid.uuid4().hex[:8]
    admin = Cliente(); admin.pedir("POST", "/api/auth/login", {"email": "admin@teste.pt", "password": "passwordsegura1"})
    conv = admin.pedir("POST", "/api/admin/convites", {"nota": sufixo, "diasValidade": 7})
    g = Cliente(); g.pedir("POST", "/api/auth/registo", {"codigo": conv["codigo"], "nome": "G",
        "email": "c-%s@t.pt" % sufixo, "password": "passwordsegura1"})
    lid = g.pedir("POST", "/api/ligas", {"nome": "Corrida %s" % sufixo, "maxEquipas": 5})["id"]
    eq = {n: g.pedir("POST", "/api/ligas/%s/equipas" % lid, {"nome": n, "treinador": "T"+n})["id"]
          for n in ["Um", "Dois"]}
    regra = {"valorInscricao": 0, "valorInicial": 1, "incremento": 1, "equipasPorEscalao": 1,
             "valorMaximo": 5, "jornadasPorBloco": 1}
    if not formula:
        regra.update({"escala": "TABELA", "tabela": "1-1€\n2-2€", "cobraTreino": True})
    g.pedir("PUT", "/api/ligas/%s/regra-divida" % lid, regra)

    j = g.pedir("POST", "/api/ligas/%s/jornadas" % lid)
    for n, p in [("Um", 3), ("Dois", 1)]:
        g.pedir("PUT", "/api/ligas/%s/jornadas/%s/resultados" % (lid, j["id"]), {"equipaId": eq[n], "pontuacao": p})

    codigos = []
    barreira = threading.Barrier(6)
    def fechar():
        barreira.wait()
        try:
            st, _ = g.pedir("POST", "/api/ligas/%s/jornadas/%s/fechar" % (lid, j["id"]), bruto=True)
            codigos.append(st)
        except Exception as e:
            codigos.append("erro")
    fios = [threading.Thread(target=fechar) for _ in range(6)]
    [f.start() for f in fios]; [f.join() for f in fios]

    total = float(g.pedir("GET", "/api/ligas/%s/equipas/%s/divida" % (lid, eq["Dois"]))["totalPendente"])
    return codigos.count(200), round(total, 2)

if __name__ == "__main__":
    for formula in (True, False):
        maus = 0
        for i in range(VEZES):
            vingaram, total = uma_vez(i, formula)
            if vingaram != 1 or total != 2.0:
                maus += 1
                print("  %s tentativa %d: %d fechos vingaram, dívida %s€ (esperado 1 e 2.0)"
                      % ("fórmula" if formula else "tabela", i + 1, vingaram, total))
        print("porta %s | %s: %d de %d corridas com cobrança a dobrar"
              % (PORTA, "fórmula" if formula else "tabela", maus, VEZES))
