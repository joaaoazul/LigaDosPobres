#!/usr/bin/env bash
# Gera uma página HTML estática com o estado da produção, para o Caddy servir
# em status.minhaquota.com (atrás de basic auth). Corre no cron a cada 5 min.
#
#   */5 * * * * cd /caminho/LigaDosPobres && ./scripts/status.sh >> status.log 2>&1

set -euo pipefail

DESTINO="${DESTINO:-./status}"
mkdir -p "$DESTINO"

estado_container() {
    docker inspect --format '{{.State.Health.Status}} (desde {{.State.StartedAt}})' "$1" 2>/dev/null \
        || echo "não encontrado"
}

app_estado=$(estado_container ligadospobres-app-1)
db_estado=$(estado_container ligadospobres-db-1)
caddy_a_correr=$(docker inspect --format '{{.State.Status}}' ligadospobres-caddy-1 2>/dev/null || echo "não encontrado")

ultimo_backup=$(grep 'backup ok' backups/backup.log 2>/dev/null | tail -1 || echo "sem registo ainda")
ultimo_r2=$(grep -E 'enviado para|falhou o upload' backups/backup.log 2>/dev/null | tail -1 || echo "sem registo ainda")

disco=$(df -h / | tail -1 | awk '{print $3 " usados de " $2 " (" $5 ")"}')
atualizado=$(date --iso-8601=seconds)

cor() {
    case "$1" in
        *healthy*) echo "#2e7d32" ;;
        *unhealthy*) echo "#c62828" ;;
        *) echo "#888" ;;
    esac
}

cat > "$DESTINO/index.html" <<HTML
<!doctype html>
<html lang="pt">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Estado — minhaquota.com</title>
<style>
  body { font-family: system-ui, sans-serif; background: #111; color: #eee; padding: 2rem; max-width: 700px; margin: 0 auto; }
  h1 { font-size: 1.3rem; }
  .linha { display: flex; justify-content: space-between; border-bottom: 1px solid #333; padding: 0.6rem 0; }
  .valor { text-align: right; }
  .rodape { color: #888; font-size: 0.8rem; margin-top: 2rem; }
</style>
</head>
<body>
<h1>Estado de minhaquota.com</h1>
<div class="linha"><span>App</span><span class="valor" style="color:$(cor "$app_estado")">$app_estado</span></div>
<div class="linha"><span>Base de dados</span><span class="valor" style="color:$(cor "$db_estado")">$db_estado</span></div>
<div class="linha"><span>Caddy</span><span class="valor">$caddy_a_correr</span></div>
<div class="linha"><span>Último backup local</span><span class="valor">$ultimo_backup</span></div>
<div class="linha"><span>Última cópia para o R2</span><span class="valor">$ultimo_r2</span></div>
<div class="linha"><span>Disco</span><span class="valor">$disco</span></div>
<p class="rodape">Gerado em $atualizado. Atualiza-se sozinho a cada 5 minutos.</p>
</body>
</html>
HTML

echo "$(date --iso-8601=seconds) página de estado gerada"
