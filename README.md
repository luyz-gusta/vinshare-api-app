# vinshare-api

API REST do projeto **Ford VIN Share**, da Challenge FIAP 2026. Cobre as disciplinas de **Arquitetura Orientada a Serviços e Web Services (SOA)** e de **Cybersecurity**.

## Equipe

| Nome | RM |
|---|---|
| Guilherme Almeida | 555180 |
| Luiz Gustavo da Silva | 558358 |
| Rafael Duarte de Freitas | 558644 |
| Rafael Gaspar Bragança Martins | 557228 |
| Vinicius Monteiro Araújo | 555088 |

## Ambiente de produção

| Recurso | URL |
|---|---|
| API base | `https://vinshare-api.azurewebsites.net/api/v1` |
| Swagger UI | [https://vinshare-api.azurewebsites.net/api/v1/docs](https://vinshare-api.azurewebsites.net/api/v1/docs) |
| OpenAPI JSON | [https://vinshare-api.azurewebsites.net/api/v1/v3/api-docs](https://vinshare-api.azurewebsites.net/api/v1/v3/api-docs) |
| Health check | [https://vinshare-api.azurewebsites.net/api/v1/actuator/health](https://vinshare-api.azurewebsites.net/api/v1/actuator/health) |

Hospedado no **Azure App Service** (plano `ASP-grchallengeford-b328`, B1 Linux, região East US). Deploy automatizado via `mvn azure-webapp:deploy` (plugin `azure-webapp-maven-plugin` 2.14.1 configurado no `pom.xml`).

Contas de demonstração:

| Email | Papel |
|---|---|
| `owner@ford.com` | `ADMIN` (operador Ford, acesso total ao dashboard analítico) |
| `analista0@ford.com` … `analista14@ford.com` | `ANALYST` (15 analistas distribuídos nas concessionárias) |
| `cliente0@email.com` … `cliente499@email.com` | `CLIENT` (500 clientes com veículo, garantia e histórico) |

Todas com senha padrão definida no seeder (`DataSeeder`). Senha real é solicitada por canal seguro, **não está versionada**.

## Stack

- Java 21
- Spring Boot 3.3
- Spring Web, Spring Data JPA, Spring Security
- PostgreSQL 15+ com Flyway
- JJWT 0.12
- springdoc-openapi (Swagger UI)
- Bucket4j (rate limit)
- Logback + logstash-logback-encoder (logs JSON)
- Lombok

## Pré-requisitos

- JDK 21
- Maven 3.9+ (ou usar Maven Wrapper depois de gerado)
- PostgreSQL 15+ rodando localmente (ou via Docker)
- Variáveis de ambiente preenchidas (ver `.env.example`)

## Configuração rápida (PostgreSQL local via Docker)

```bash
docker run --name vinshare-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=vinshare \
  -p 5432:5432 \
  -d postgres:15
```

## Como rodar

```bash
# 1. Copie e ajuste as variáveis
cp .env.example .env

# 2. Suba a aplicação (Flyway aplica as migrations automaticamente)
mvn spring-boot:run
```

Endpoints úteis após subir:

- Swagger UI: <http://localhost:8080/api/v1/docs>
- OpenAPI JSON: <http://localhost:8080/api/v1/api-docs>
- Health: <http://localhost:8080/api/v1/actuator/health>

## Estrutura de pacotes

```
com.fiap.vinshare
├── VinShareApiApplication.java
├── config/                  configurações (security, CORS, Swagger)
│   └── swagger/
├── controllers/             implementação dos endpoints
├── domain/
│   ├── dto/                 DTOs agrupados por feature
│   └── entities/            entidades JPA
├── infra/
│   ├── errors/              handler global de exceções + tipos próprios
│   │   └── exceptions/
│   ├── responses/           envelopes de resposta padrão
│   │   └── details/
│   └── security/            JwtAuthenticationFilter, JwtService, RateLimitFilter
├── repositories/            interfaces Spring Data JPA
├── service/                 regras de negócio
└── specs/                   interfaces de controller com anotações Swagger
    └── error/               annotations @ApiResponse* compartilhadas
```

## Banco de dados

- Migrations em `src/main/resources/db/migration/`.
- Próximas migrations seguem o padrão `V<timestamp>__descricao.sql`.

## Segurança (resumo)

- JWT com access curto (15 min) e refresh longo (7 dias).
- BCrypt cost 12 nas senhas.
- RBAC com papéis `CLIENT`, `ANALYST`, `ADMIN`.
- Bean Validation em todos os DTOs.
- Rate limit no login e no chat (Bucket4j).
- CORS restrito (env `CORS_ORIGINS`).
- ProblemDetails (RFC 7807) sem stack trace.
- CPF em AES-GCM + HMAC para lookup.
- Logs JSON com `correlationId` e campos sensíveis mascarados.
- Audit log para ações críticas.

