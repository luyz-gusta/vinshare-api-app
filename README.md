# vinshare-api

[![DevSecOps](https://github.com/luyz-gusta/vinshare-api-app/actions/workflows/devsecops.yml/badge.svg)](https://github.com/luyz-gusta/vinshare-api-app/actions/workflows/devsecops.yml)

API REST do **Ford VIN Share** (Challenge FIAP 2026). Aumenta a retenção de clientes no pós-venda da rede Ford: VIN Share é a porcentagem de veículos Ford que fazem manutenção na rede oficial. Cobre as disciplinas de **Arquitetura Orientada a Serviços e Web Services** e de **Cybersecurity**.

- **Arquitetura, diagramas, fluxo de autenticação, matriz de perfis e endpoints:** [docs/arquitetura.md](docs/arquitetura.md)
- **Consultas de monitoramento (Log Analytics):** [docs/observabilidade/consultas.kql](docs/observabilidade/consultas.kql)

## Equipe

| Nome | RM |
|---|---|
| Guilherme Almeida | 555180 |
| Luiz Gustavo da Silva | 558358 |
| Rafael Duarte de Freitas | 558644 |
| Rafael Gaspar Bragança Martins | 557228 |
| Vinicius Monteiro Araújo | 555088 |

## Produção

| Recurso | URL |
|---|---|
| API base | `https://vinshare-api.azurewebsites.net/api/v1` |
| Swagger UI | https://vinshare-api.azurewebsites.net/api/v1/docs |
| OpenAPI JSON | https://vinshare-api.azurewebsites.net/api/v1/v3/api-docs |
| Health check | https://vinshare-api.azurewebsites.net/api/v1/actuator/health |

Hospedagem: Azure App Service (Linux, Java 21, East US).

O pipeline tem o job `deploy` (GitHub Actions → OIDC → App Service, com aprovação manual e smoke test). Ele só passa a publicar depois da configuração da identidade OIDC no Azure; até lá o deploy continua manual, com `./mvnw azure-webapp:deploy`.

**Contas de demonstração:**
- `owner@ford.com` (ADMIN);
- `analista0@ford.com` … `analista14@ford.com` (ANALYST);
- `cliente0@email.com` … `cliente499@email.com` (CLIENT).

A senha é entregue aos professores pelo Teams e **não fica no repositório**.

## Stack

- Java 21, Spring Boot 3.5, Spring Web, Spring Data JPA, Spring Security
- PostgreSQL 15+ com Flyway
- JJWT 0.12 (HS512), BCrypt 12, AES-256-GCM (CPF)
- springdoc-openapi (Swagger UI)
- Bucket4j + Caffeine (rate limit e bloqueio de força bruta), Micrometer
- Logback + logstash-logback-encoder (logs JSON mascarados)
- JUnit 5, MockMvc, Testcontainers, JaCoCo

## Como rodar localmente

**Pré-requisitos:** JDK 21 e Docker Desktop **em execução**. Não é preciso instalar o Maven: use `./mvnw` (ou `mvnw.cmd` no Windows).

```bash
# 1. Banco
docker run --name vinshare-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=vinshare -p 5432:5432 -d postgres:16-alpine

# 2. Variáveis (preencha os segredos, ver a tabela abaixo)
cp .env.example .env

# 3. Subir (o Flyway aplica as migrations)
./mvnw spring-boot:run
```

**Variáveis de ambiente:**

| Variável | Obrigatória | Como gerar / valor |
|---|---|---|
| `JWT_SECRET` | sim (≥ 64 caracteres) | `python -c "import secrets; print(secrets.token_urlsafe(64))"` |
| `CPF_AES_KEY` | sim (32 bytes Base64) | `openssl rand -base64 32` |
| `CPF_HMAC_KEY` | sim (≥ 32 caracteres) | `python -c "import secrets; print(secrets.token_urlsafe(48))"` |
| `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` | sim | Veja o `.env.example` |
| `SEED_ENABLED` / `SEED_DEFAULT_PASSWORD` | não / sim, se o seed estiver ligado | `true` popula 500 clientes sintéticos; senha com ≥ 12 caracteres |
| `CORS_ORIGINS` | não | Origens separadas por vírgula |
| `CLIENT_IP_TRUSTED_HOPS` | não | `0` local; no Azure App Service a API assume `1` sozinha |
| `GEMINI_API_KEY`, `EXPO_ACCESS_TOKEN` | não | Vazias ativam o modo simulado |
| `RETENTION_ENABLED` | não | `true` em produção |

A aplicação **não sobe** sem `JWT_SECRET`, `CPF_AES_KEY` e `CPF_HMAC_KEY`. Não existe chave padrão.

**URLs locais:**
- Swagger: http://localhost:8080/api/v1/docs
- OpenAPI: http://localhost:8080/api/v1/v3/api-docs
- Health: http://localhost:8080/api/v1/actuator/health

**Como autenticar no Swagger:**
1. Abra `POST /auth/login` → *Try it out* e envie e-mail e senha.
2. Copie o `data.accessToken`.
3. Clique em **Authorize** e cole o token (sem o prefixo "Bearer").

### Rodando com Docker

```bash
docker compose up --build   # banco + API; lê os segredos do .env
```

A imagem roda com usuário sem privilégio (uid 10001), só com o JRE e com healthcheck.

## Testes

```bash
./mvnw verify   # requer Docker em execução (Testcontainers com postgres:16-alpine)
```

- Relatório JUnit: `target/surefire-reports/`
- Cobertura: `target/site/jacoco/index.html`
- No GitHub, cada execução do pipeline publica o relatório "Testes JUnit" e o resumo de cobertura.

| Classe | O que garante |
|---|---|
| `AuthFlowTest` | Cadastro, login, refresh com rotação e detecção de reuso, logout, bloqueio por força bruta, consentimento LGPD, CPF válido |
| `ChangePasswordTest` | Troca de senha: 204, 422 sem derrubar a sessão, 429 após 5 erros, encerramento das outras sessões |
| `ErrorHandlingTest` | 400, 404, 405, 401 (sem token, token inválido, token expirado), sempre em ProblemDetail |
| `AppointmentFlowTest` | 201 + Location, 422 no passado, 409 em conflito, 403 para outra concessionária, limite de valor, fluxo completo com pontos |
| `ProfilesTest` | Responsabilidades de ADMIN e ANALYST, catálogo de prêmios, métricas só para ADMIN |
| `OwnershipTest` | OWASP API1: CLIENT só vê o que é dele, ANALYST só vê a própria concessionária, paginação de leads |
| `OpenApiContractTest` | Swagger documenta os status reais e o schema ProblemDetail |
| `RateLimitTest`, `SecurityHeadersTest` | 429 com Retry-After, X-Forwarded-For forjado não burla o limite, HSTS/CSP/CORS, correlation ID |
| `LoyaltyRedeemConcurrencyTest`, `LoginConcurrencyTest`, `RefreshConcurrencyTest` | Mais requisições simultâneas que conexões no pool: resgates não gastam o mesmo saldo, rajada de logins inválidos responde 401 sem travar o pool, refresh simultâneo renova uma única vez |
| `VehicleAndDeviceTest` | Odômetro não regride; token de push de outra conta é recusado |
| `ChatAndLoyaltyFlowTest` | 201 no chat e no resgate; CPF e e-mail não chegam ao provedor de IA |
| `DataSubjectRightsTest`, `RetentionTest` | Exportação e anonimização (LGPD art. 18) com reautenticação, retenção de tokens |
| `SecurityEventsTest` | Métricas de segurança e auditoria de acesso a dados pessoais |
| Unitários (`JwtServiceTest`, `CryptoServiceTest`, `ClientIpResolverTest`, `LoginAttemptServiceTest`, `PiiRedactorTest`, `LeadHealthStatusTest`) | Regras isoladas |

## Pipeline DevSecOps

`.github/workflows/devsecops.yml`, executado em todo PR e em todo push na `main`:

| Job | Ferramenta | Bloqueia quando |
|---|---|---|
| `secret-scan` | Gitleaks (histórico completo) | Segredo encontrado |
| `sast` | Semgrep (Java, OWASP Top 10, JWT, secrets) | Achado de severidade ERROR |
| `build-test` | Maven + JUnit + Testcontainers + JaCoCo + SBOM CycloneDX | Teste falha |
| `sca` | Trivy sobre o SBOM | CVE CRITICAL/HIGH com correção disponível |
| `container` | Hadolint + Trivy da imagem | Dockerfile fora do padrão ou CVE CRITICAL/HIGH na imagem |
| `iac-scan` | Checkov (Bicep, Dockerfile, Actions) | Configuração insegura |
| `deploy` | azure/login (OIDC) + webapps-deploy + smoke test | Aprovação negada ou health diferente de UP |

Além disso:
- Dependabot semanal (Maven e Actions);
- DAST passivo semanal (`dast.yml`, ZAP baseline);
- IaC do App Service em [`infra/main.bicep`](infra/main.bicep);
- provisionamento do monitoramento em [`scripts/azure-monitoring.sh`](scripts/azure-monitoring.sh).

## Laboratório IoT

[`iot/`](iot/) demonstra MQTT sobre TLS mútuo para telemetria de odômetro:
- broker Mosquitto só na porta 8883;
- certificado por veículo, com o CN igual ao VIN;
- ACL por tópico.

Como rodar:

```bash
cd iot && bash certs/gerar-certificados.sh && docker compose up -d
cd simulador && pip install -r requirements.txt
python assinar_telemetria.py &                  # ingestão (só leitura)
python publicar_telemetria.py 9BFTESTE000000001 # veículo publica a própria telemetria
```

## Estrutura de pacotes

```
com.fiap.vinshare
├── config/              segurança, CORS, Swagger, agendamento
├── controllers/         endpoints (implementam specs/)
├── specs/               contrato OpenAPI (+ error/ com respostas de erro)
├── service/             regras de negócio, CustomerAccessPolicy, PrivacyService
├── repositories/        Spring Data JPA
├── domain/              entidades JPA e DTOs (records)
└── infra/
    ├── errors/          GlobalExceptionHandler, ProblemDetails, exceções
    ├── responses/       envelope ApiSingleResponse
    ├── security/        JWT, rate limit, IP real, criptografia, eventos, redação de PII
    └── seed/            dados sintéticos de demonstração
```

## Banco de dados

As migrations ficam em `src/main/resources/db/migration/` (`V1` a `V4`). A próxima deve ser `V5__descricao.sql`.

## Segurança (resumo)

- **Tokens:** JWT HS512 de 15 min com `iss`, `aud` e `jti`. O papel é recarregado do banco a cada requisição. Refresh token opaco de 7 dias, de uso único (rotação com lock), com detecção de reuso e tolerância de 30 s para renovações simultâneas do app.
- **Senhas e força bruta:** BCrypt custo 12. Rate limit por IP real (login, cadastro, refresh, chat e geral), mais bloqueio por e-mail após 5 falhas em 10 min, válido também para a troca de senha.
- **Troca de senha:** `PATCH /me/password` exige a senha atual e encerra todas as sessões (refresh tokens). O app pede novo login quando o access token atual expira, em até 15 min.
- **Autorização:** RBAC `CLIENT` / `ANALYST` / `ADMIN`, com escopo por dono e por concessionária (404 fora do escopo).
- **Dados pessoais:** CPF cifrado com AES-256-GCM e busca por HMAC-SHA256. Segredos obrigatórios, sem valor padrão. Dados pessoais removidos antes de chegar à IA.
- **Erros:** RFC 7807 em tudo, inclusive 401, 403 e 429, sem stack trace.
- **Headers:** HSTS, CSP, X-Frame-Options, nosniff, Referrer-Policy, Permissions-Policy. CORS restrito e sem credenciais.
- **Observabilidade:** logs JSON com `correlationId` e mascaramento, métricas de segurança, trilha `audit_log` e alertas no Azure Monitor.
- **LGPD:** consentimento obrigatório, exportação e anonimização dos dados do titular (a exclusão exige a senha atual), retenção automática.
