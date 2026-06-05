#!/usr/bin/env bash
# ============================================================
#  update-fleet.sh — atualiza TODAS as VMs de clientes de uma vez
#
#  A imagem é buildada UMA vez no CI (GHCR). Este script só manda cada VM
#  PUXAR a mesma imagem e reiniciar (o Flyway migra o banco de cada cliente
#  automaticamente no boot do container novo).
#
#  Uso:
#    scripts/update-fleet.sh                       # atualiza todos p/ 'latest'
#    scripts/update-fleet.sh --version v1.4.0      # pin numa release
#    scripts/update-fleet.sh --canary              # só a 1ª VM (valide, depois rode de novo)
#    scripts/update-fleet.sh --file outra-lista.txt
#
#  Inventário (scripts/fleet.txt), uma VM por linha:
#    usuario@host[:/caminho/do/repo]      # caminho default: ~/erp-construtora
#    # linhas começando com # são ignoradas
#
#  Requer: chave SSH configurada para cada host; Docker/Compose já instalados na VM.
# ============================================================
set -euo pipefail

VERSION=""
FLEET_FILE="$(dirname "$0")/fleet.txt"
CANARY=0
COMPOSE="docker-compose.registry.yml"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version) VERSION="$2"; shift 2 ;;
    --file)    FLEET_FILE="$2"; shift 2 ;;
    --canary)  CANARY=1; shift ;;
    -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
    *) echo "Opção desconhecida: $1" >&2; exit 2 ;;
  esac
done

[[ -f "$FLEET_FILE" ]] || { echo "ERRO: inventário não encontrado: $FLEET_FILE" >&2; exit 1; }

# Lê entradas válidas (ignora comentários e linhas vazias)
mapfile -t ENTRIES < <(grep -vE '^\s*(#|$)' "$FLEET_FILE")
[[ ${#ENTRIES[@]} -gt 0 ]] || { echo "ERRO: nenhuma VM no inventário." >&2; exit 1; }

if [[ $CANARY -eq 1 ]]; then
  ENTRIES=("${ENTRIES[0]}")
  echo ">> Modo CANÁRIO: atualizando apenas a primeira VM."
fi

echo ">> Alvo: ${#ENTRIES[@]} VM(s) | versão: ${VERSION:-latest}"
ok=0; fail=0; failed_hosts=()

for entry in "${ENTRIES[@]}"; do
  host="${entry%%:*}"
  dir="${entry#*:}"; [[ "$dir" == "$entry" ]] && dir="~/erp-construtora"

  echo ""
  echo "================================================================"
  echo ">> $host  (dir: $dir)"
  echo "================================================================"

  # Comando remoto: fixa APP_VERSION (se passado), puxa a imagem e sobe.
  remote="cd \"$dir\""
  if [[ -n "$VERSION" ]]; then
    remote+=" && { grep -q '^APP_VERSION=' .env && sed -i 's/^APP_VERSION=.*/APP_VERSION=$VERSION/' .env || echo 'APP_VERSION=$VERSION' >> .env; }"
  fi
  remote+=" && docker compose -f $COMPOSE pull"
  remote+=" && docker compose -f $COMPOSE up -d"
  remote+=" && docker image prune -f >/dev/null 2>&1 || true"

  if ssh -o ConnectTimeout=15 "$host" "bash -lc '$remote'"; then
    # Health: aguarda e confirma que o backend ficou de pé (não reiniciando)
    sleep 20
    state="$(ssh "$host" "docker inspect -f '{{.State.Status}}' construtora-backend 2>/dev/null || echo unknown")"
    if [[ "$state" == "running" ]]; then
      echo ">> [OK] $host — backend running"
      ok=$((ok+1))
    else
      echo ">> [FALHA] $host — backend em estado '$state' (verifique: docker compose -f $COMPOSE logs backend)" >&2
      fail=$((fail+1)); failed_hosts+=("$host")
    fi
  else
    echo ">> [FALHA] $host — comando de atualização retornou erro" >&2
    fail=$((fail+1)); failed_hosts+=("$host")
  fi
done

echo ""
echo "================================================================"
echo ">> Resultado: $ok atualizada(s), $fail com falha."
[[ $fail -gt 0 ]] && { printf '   falhou: %s\n' "${failed_hosts[@]}"; }
if [[ $CANARY -eq 1 && $fail -eq 0 ]]; then
  echo ">> Canário OK. Valide o cliente e rode de novo SEM --canary para o restante."
fi
[[ $fail -eq 0 ]] || exit 1
