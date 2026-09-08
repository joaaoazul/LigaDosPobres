#!/usr/bin/env bash
# Cópia de segurança de uma base de dados alojada numa plataforma — Railway,
# Fly, Render, qualquer uma que dê um URL de ligação. O irmão deste script, o
# backup.sh, fala com o contentor do docker compose e por isso só serve no
# servidor próprio; este só precisa do URL, e corre de qualquer máquina que o
# alcance (o teu portátil serve, e é o melhor sítio: um backup que só existe
# na plataforma que pode desaparecer não é um backup).
#
#   export DATABASE_URL='postgresql://utilizador:senha@host:porta/base'
#   ./scripts/backup-railway.sh
#
# No Railway o URL público está no separador Variables do serviço Postgres,
# em DATABASE_PUBLIC_URL. O interno (postgres.railway.internal) não é
# alcançável de fora, só de dentro do projecto.
#
# Um backup que nunca foi restaurado não é um backup, é uma suposição. Depois
# de fazer o primeiro, corre o modo de verificação pelo menos uma vez:
#
#   ./scripts/backup-railway.sh --verificar backups/ligadospobres-AAAAMMDD-HHMMSS.sql.gz
#
# Ele restaura o ficheiro para uma base descartável e conta o que lá ficou.
# Se as contas baterem certo com o que a aplicação mostra, o backup presta.

set -euo pipefail

DESTINO="${DESTINO:-./backups}"
DIAS_A_MANTER="${DIAS_A_MANTER:-30}"

# A base descartável do modo de verificação. Por omissão um PostgreSQL local;
# nunca a de produção — o restauro apaga o que lá estiver à frente.
URL_VERIFICACAO="${URL_VERIFICACAO:-postgresql://postgres:postgres@localhost:5432/postgres}"

# As tabelas que interessa contar: quem existe e quanto se deve. Se estas
# vierem certas, o resto veio atrás.
TABELAS=(gestor liga equipa treinador jornada resultado_jornada divida bloco_divida)


verificar() {
    local ficheiro="$1"
    [ -f "$ficheiro" ] || { echo "ERRO: não encontrei $ficheiro" >&2; exit 1; }

    # Nome irrepetível: duas verificações ao mesmo tempo não se pisam, e fica
    # claro que a base é descartável.
    local base="verificacao_$(date +%Y%m%d%H%M%S)_$$"
    local servidor="${URL_VERIFICACAO%/*}"

    echo "A restaurar $ficheiro para a base descartável $base…"
    psql "$URL_VERIFICACAO" -q -c "create database $base" >/dev/null

    # O restauro fica sempre limpo, mesmo que rebente a meio.
    trap 'psql "$URL_VERIFICACAO" -q -c "drop database if exists '"$base"'" >/dev/null 2>&1 || true' EXIT

    # ON_ERROR_STOP: sem isto o psql engole os erros e sai com 0, e uma
    # verificação que não falha nunca não verifica nada.
    if ! gunzip -c "$ficheiro" | psql "$servidor/$base" -q -v ON_ERROR_STOP=1 >/dev/null; then
        echo "ERRO: o restauro falhou. Este backup NÃO presta." >&2
        exit 1
    fi

    echo
    printf '%-20s %s\n' "TABELA" "LINHAS"
    local total=0
    for tabela in "${TABELAS[@]}"; do
        local linhas
        linhas=$(psql "$servidor/$base" -Atc "select count(*) from $tabela")
        printf '%-20s %s\n' "$tabela" "$linhas"
        total=$((total + linhas))
    done

    # A versão do esquema: um backup restaurado que não sabe em que migração
    # está é um backup que a aplicação vai recusar arrancar em cima.
    echo
    echo "Migração mais recente: $(psql "$servidor/$base" -Atc \
        "select version || ' — ' || description from flyway_schema_history order by installed_rank desc limit 1")"

    if [ "$total" -eq 0 ]; then
        echo "ERRO: restaurou, mas não há uma única linha. Isto não é um backup." >&2
        exit 1
    fi
    echo
    echo "Backup verificado: restaura e tem dados."
}


fazer_backup() {
    : "${DATABASE_URL:?Falta o DATABASE_URL. No Railway é o DATABASE_PUBLIC_URL do serviço Postgres.}"

    mkdir -p "$DESTINO"
    local ficheiro="$DESTINO/ligadospobres-$(date +%Y%m%d-%H%M%S).sql.gz"

    # --clean --if-exists: o ficheiro consegue reescrever uma base já existente
    #   em vez de falhar a meio com "já existe".
    # --no-owner --no-privileges: o dono das tabelas no Railway é o utilizador
    #   do Railway, e no teu servidor é o "liga". Sem isto, o restauro na outra
    #   ponta enche-se de erros por causa de um papel que lá não existe — e é
    #   precisamente para mudar de casa que este ficheiro serve.
    pg_dump "$DATABASE_URL" \
        --clean --if-exists --no-owner --no-privileges \
      | gzip > "$ficheiro"

    # Um dump vazio ou truncado é pior do que nenhum, porque dá falsa segurança.
    local tamanho
    tamanho=$(stat -c%s "$ficheiro")
    if [ "$tamanho" -lt 1024 ]; then
        echo "ERRO: o backup tem apenas ${tamanho} bytes. Ficheiro removido." >&2
        rm -f "$ficheiro"
        exit 1
    fi

    find "$DESTINO" -name 'ligadospobres-*.sql.gz' -mtime "+$DIAS_A_MANTER" -delete

    echo "$(date --iso-8601=seconds) backup ok: $ficheiro ($((tamanho / 1024)) KB)"
}


case "${1:-}" in
    --verificar) verificar "${2:?Falta o ficheiro a verificar.}" ;;
    "")          fazer_backup ;;
    *)           echo "Uso: $0 [--verificar ficheiro.sql.gz]" >&2; exit 2 ;;
esac
