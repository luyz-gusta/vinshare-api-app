#!/usr/bin/env bash
#
# Envia as variáveis do .env local como Application Settings do App Service
# vinshare-api. Os valores são lidos do .env em tempo de execução: este script
# não contém segredos e é seguro versionar.
#
# Pré-requisitos:
#   1. Azure CLI instalado:   brew install azure-cli
#   2. Login:                  az login
#
# Uso:
#   ./scripts/azure-appsettings.sh
#
set -euo pipefail

RESOURCE_GROUP="gr_challenge_ford"
APP_NAME="vinshare-api"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Erro: $ENV_FILE não encontrado." >&2
  exit 1
fi

# Variáveis que NÃO devem ir para o Azure:
# SERVER_PORT é sobrescrito pelo runtime do App Service (-Dserver.port=80).
SKIP_KEYS=("SERVER_PORT")

settings=()
while IFS= read -r line || [[ -n "$line" ]]; do
  # Ignora comentários e linhas em branco.
  [[ "$line" =~ ^[[:space:]]*# ]] && continue
  [[ -z "${line//[[:space:]]/}" ]] && continue
  # Quebra na primeira ocorrência de '=' (preserva '=' dentro do valor, ex.: base64).
  key="${line%%=*}"
  value="${line#*=}"
  key="${key//[[:space:]]/}"

  skip=false
  for s in "${SKIP_KEYS[@]}"; do
    [[ "$key" == "$s" ]] && skip=true && break
  done
  $skip && continue

  settings+=("${key}=${value}")
done < "$ENV_FILE"

echo "Aplicando ${#settings[@]} app settings em ${APP_NAME} (${RESOURCE_GROUP})..."
az webapp config appsettings set \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APP_NAME" \
  --settings "${settings[@]}" \
  --output table

echo "Reiniciando o app para aplicar..."
az webapp restart --resource-group "$RESOURCE_GROUP" --name "$APP_NAME"

echo "Pronto. Acompanhe o boot em: az webapp log tail -g ${RESOURCE_GROUP} -n ${APP_NAME}"
