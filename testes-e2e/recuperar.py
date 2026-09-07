"""Recuperação de password, do pedido ao link, e o reinício da aplicação."""
import re, sys, uuid, json, urllib.request, urllib.error, http.cookiejar
exec(open("/tmp/e2e.py").read().split('def correr(')[0])

LOG = "/tmp/app.log"
sufixo = uuid.uuid4().hex[:8]
email = "rec-%s@t.pt" % sufixo

admin = Cliente()
admin.pedir("POST", "/api/auth/login", {"email": "admin@teste.pt", "password": "passwordsegura1"})
convite = admin.pedir("POST", "/api/admin/convites", {"nota": sufixo, "diasValidade": 7})

gestor = Cliente()
gestor.pedir("POST", "/api/auth/registo", {"codigo": convite["codigo"], "nome": "Rec",
             "email": email, "password": "passwordsegura1"})
lid = gestor.pedir("POST", "/api/ligas", {"nome": "Rec %s" % sufixo, "maxEquipas": 3})["id"]
eqid = gestor.pedir("POST", "/api/ligas/%s/equipas" % lid, {"nome": "Alfa", "treinador": "TA"})["id"]

marca = len(open(LOG, encoding="utf-8", errors="replace").read())
st, _ = Cliente().pedir("POST", "/api/auth/recuperar", {"email": email}, bruto=True)
igual("pedido de recuperação responde 204", st, 204)

texto = open(LOG, encoding="utf-8", errors="replace").read()[marca:]
achado = re.search(r"nova-password\.html\?codigo=(\S+)", texto)
verificar("o link saiu (no log, sem chave de email)", bool(achado))
codigo = achado.group(1) if achado else None

if codigo:
    st, _ = Cliente().pedir("POST", "/api/auth/recuperar/confirmar",
                            {"codigo": codigo, "password": "curta"}, bruto=True)
    igual("password nova curta recusada", st, 400)

    st, _ = Cliente().pedir("POST", "/api/auth/recuperar/confirmar",
                            {"codigo": codigo, "password": "passwordnovasegura"}, bruto=True)
    igual("redefinir com o código do link", st, 204)

    st, _ = gestor.pedir("GET", "/api/ligas", bruto=True)
    igual("a sessão antiga foi cortada", st, 401)

    novo = Cliente()
    st, _ = novo.pedir("POST", "/api/auth/login", {"email": email, "password": "passwordnovasegura"}, bruto=True)
    igual("entra-se com a nova", st, 200)

    st, _ = Cliente().pedir("POST", "/api/auth/login", {"email": email, "password": "passwordsegura1"}, bruto=True)
    igual("a antiga deixou de servir", st, 401)

    st, _ = Cliente().pedir("POST", "/api/auth/recuperar/confirmar",
                            {"codigo": codigo, "password": "outrapasswordsegura"}, bruto=True)
    igual("o código serve uma vez só", st, 400)

    # estado para o teste de reinício
    json.dump({"email": email, "password": "passwordnovasegura", "liga": lid, "equipa": eqid},
              open("/tmp/estado_reinicio.json", "w"))

print("\n%d verificações passaram, %d falharam" % (passou[0], len(falhas)))
for f in falhas: print("  FALHA:", f)
sys.exit(1 if falhas else 0)
