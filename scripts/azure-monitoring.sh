#!/usr/bin/env bash
#
# Provisiona o monitoramento do vinshare-api: Log Analytics, Application Insights
# (agente Java sem mudança de código), diagnóstico do App Service, health check,
# grupo de ação e alertas de disponibilidade e de segurança.
#
# Uso: ALERT_EMAIL=equipe@exemplo.com ./scripts/azure-monitoring.sh
#
set -euo pipefail
export MSYS_NO_PATHCONV=1

RG="gr_challenge_ford"
APP="vinshare-api"
LOCATION="eastus"
WS="vinshare-logs"
AI="vinshare-appinsights"
AG="vinshare-alertas"
ALERT_EMAIL="${ALERT_EMAIL:?Defina ALERT_EMAIL=seu-email}"

az extension add --name application-insights --upgrade --only-show-errors
az extension add --name scheduled-query --upgrade --only-show-errors

echo "1/7 Log Analytics + Application Insights"
az monitor log-analytics workspace create -g "$RG" -n "$WS" -l "$LOCATION" --retention-time 30 -o none
WS_ID=$(az monitor log-analytics workspace show -g "$RG" -n "$WS" --query id -o tsv)
az monitor app-insights component create -g "$RG" --app "$AI" -l "$LOCATION" --workspace "$WS_ID" --kind web -o none
AI_CONN=$(az monitor app-insights component show -g "$RG" --app "$AI" --query connectionString -o tsv)

echo "2/7 Agente Java do Application Insights + profile prod (logs JSON) + IP real"
az webapp config appsettings set -g "$RG" -n "$APP" --settings \
  APPLICATIONINSIGHTS_CONNECTION_STRING="$AI_CONN" \
  ApplicationInsightsAgent_EXTENSION_VERSION="~3" \
  SPRING_PROFILES_ACTIVE="prod" \
  CLIENT_IP_TRUSTED_HOPS="1" \
  RETENTION_ENABLED="true" -o none

echo "3/7 Logs do container e HTTP -> Log Analytics"
az webapp log config -g "$RG" -n "$APP" --docker-container-logging filesystem -o none
APP_ID=$(az webapp show -g "$RG" -n "$APP" --query id -o tsv)
az monitor diagnostic-settings create --name vinshare-diag --resource "$APP_ID" --workspace "$WS_ID" \
  --logs '[{"category":"AppServiceConsoleLogs","enabled":true},{"category":"AppServiceHTTPLogs","enabled":true},{"category":"AppServiceAppLogs","enabled":true}]' \
  --metrics '[{"category":"AllMetrics","enabled":true}]' -o none

echo "4/7 Health check do App Service"
az webapp config set -g "$RG" -n "$APP" --generic-configurations '{"healthCheckPath":"/api/v1/actuator/health"}' -o none

echo "5/7 Grupo de ação (e-mail)"
az monitor action-group create -g "$RG" -n "$AG" --short-name vinshare --action email equipe "$ALERT_EMAIL" -o none
AG_ID=$(az monitor action-group show -g "$RG" -n "$AG" --query id -o tsv)

echo "6/7 Alertas de disponibilidade e desempenho"
az monitor metrics alert create -g "$RG" -n "api-http-5xx" --scopes "$APP_ID" \
  --condition "total Http5xx > 10" --window-size 5m --evaluation-frequency 1m --severity 1 \
  --action "$AG_ID" --description "Mais de 10 respostas 5xx em 5 minutos" -o none
az monitor metrics alert create -g "$RG" -n "api-latencia" --scopes "$APP_ID" \
  --condition "avg HttpResponseTime > 2" --window-size 5m --evaluation-frequency 1m --severity 2 \
  --action "$AG_ID" --description "Tempo médio de resposta acima de 2 s" -o none
az monitor metrics alert create -g "$RG" -n "api-health" --scopes "$APP_ID" \
  --condition "avg HealthCheckStatus < 100" --window-size 5m --evaluation-frequency 1m --severity 1 \
  --action "$AG_ID" --description "Health check abaixo de 100%" -o none

echo "7/7 Alertas de segurança (consultas de log)"
EVENT_QUERY='AppServiceConsoleLogs | extend log = parse_json(ResultDescription)'
az monitor scheduled-query create -g "$RG" -n "seg-forca-bruta-login" --scopes "$WS_ID" \
  --condition "count 'FalhasLogin' > 10" \
  --condition-query FalhasLogin="$EVENT_QUERY | where tostring(log.event) == 'LOGIN_FAILURE'" \
  --window-size 5m --evaluation-frequency 5m --severity 2 --action-groups "$AG_ID" \
  --description "Mais de 10 falhas de login em 5 minutos (possível força bruta)" -o none
az monitor scheduled-query create -g "$RG" -n "seg-reuso-refresh-token" --scopes "$WS_ID" \
  --condition "count 'Reuso' > 0" \
  --condition-query Reuso="$EVENT_QUERY | where tostring(log.event) == 'REFRESH_TOKEN_REUSE'" \
  --window-size 5m --evaluation-frequency 5m --severity 1 --action-groups "$AG_ID" \
  --description "Refresh token reutilizado (possível roubo de sessão)" -o none
az monitor scheduled-query create -g "$RG" -n "seg-rate-limit" --scopes "$WS_ID" \
  --condition "count 'Limitadas' > 50" \
  --condition-query Limitadas="$EVENT_QUERY | where tostring(log.event) == 'RATE_LIMITED'" \
  --window-size 5m --evaluation-frequency 5m --severity 3 --action-groups "$AG_ID" \
  --description "Mais de 50 requisições bloqueadas por rate limit em 5 minutos" -o none

az webapp restart -g "$RG" -n "$APP"
echo "Pronto. Workspace: $WS | App Insights: $AI | Alertas enviados para $ALERT_EMAIL"
