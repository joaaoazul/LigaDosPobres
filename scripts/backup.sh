#!/usr/bin/env bash
# Cópia de segurança da base de dados, para correr no cron do servidor.
#
#   0 4 * * *  cd /caminho/LigaDosPobres && ./scripts/backup.sh >> backups/backup.log 2>&1
#
# Um backup que nunca foi restaurado não é um backup. Testa o restauro pelo
# menos uma vez, com o comando que está no fim deste ficheiro.

set -euo pipefail

DIAS_A_MANTER="${DIAS_A_MANTER:-14}"
DESTINO="${DESTINO:-./backups}"
COMPOSE="${COMPOSE:-docker compose -f compose.prod.yml}"

mkdir -p "$DESTINO"
ficheiro="$DESTINO/ligadospobres-$(date +%Y%m%d-%H%M%S).sql.gz"

# --clean --if-exists: o ficheiro resultante consegue reescrever uma base de
# dados já existente, em vez de falhar a meio com "já existe".
$COMPOSE exec -T db pg_dump \
    --username "${POSTGRES_USER:-liga}" \
    --dbname "${POSTGRES_DB:-ligadospobres}" \
    --clean --if-exists \
  | gzip > "$ficheiro"

# Um dump vazio ou truncado é pior do que nenhum, porque dá falsa segurança.
tamanho=$(stat -c%s "$ficheiro")
if [ "$tamanho" -lt 1024 ]; then
    echo "ERRO: o backup tem apenas ${tamanho} bytes. Ficheiro removido." >&2
    rm -f "$ficheiro"
    exit 1
fi

find "$DESTINO" -name 'ligadospobres-*.sql.gz' -mtime "+$DIAS_A_MANTER" -delete

echo "$(date --iso-8601=seconds) backup ok: $ficheiro ($((tamanho / 1024)) KB)"

# Restaurar:
#   gunzip -c backups/ligadospobres-AAAAMMDD-HHMMSS.sql.gz \
#     | docker compose -f compose.prod.yml exec -T db psql -U liga -d ligadospobres
