# acs-backend

The Access Control Service (ACS) backend — a centralized security and governance middleware for managing access policies across heterogeneous data catalogs and identity providers.

## Features

- **Dynamic OIDC Authentication**: Pluggable identity provider integration via OIDC discovery (Keycloak, Okta, Azure AD).
- **Persona-Based RBAC**: Fine-grained authorization using personas (ADMIN, GOVERNANCE_ADMIN, APPROVER, REQUESTER, AUDITOR) with priority-based resolution (DB > Group > JWT).
- **Generic Catalog Interface**: Traversal and policy overlay for Unity Catalog, Hive Metastore, AWS Glue, and more via a pluggable SPI.
- **Access Request Workflow**: Automated submission, multi-level approval, and verification with lineage tracking.
- **Audit & Governance**: Full audit trail with OpenLineage-compatible event emission.
- **Observability**: Built-in support for OpenTelemetry tracing and Prometheus metrics.
- **Security**: Backend-for-Frontend (BFF) JWT pattern with signed session cookies.

## Architecture

Detailed C4 diagrams (Context, Container, Component), UML class diagrams, OIDC sequence flows, and persona resolution logic:

📄 **[docs/architecture.md](docs/architecture.md)**

OIDC transition analysis and effort estimation:

📄 **[docs/oidc-transition-analysis.md](docs/oidc-transition-analysis.md)**

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+ (or use the included `mvnw` wrapper)
- PostgreSQL (or use H2 in dev/test mode)

### Development Mode
```shell
./mvnw compile quarkus:dev
```
> Dev UI available at http://localhost:8080/q/dev/
> Swagger UI available at http://localhost:8080/q/swagger-ui/

### Running Tests
```shell
./mvnw test
```

Tests include:
- **166 test cases** across 16 test classes
- **Cucumber BDD** scenarios for end-to-end flows
- **JaCoCo** code coverage with 80% line/branch threshold

### Environment Variables

| Variable | Description | Default |
|:---|:---|:---|
| `OIDC_AUTH_SERVER_URL` | OIDC discovery endpoint | `http://localhost:8180/realms/acs` |
| `OIDC_CLIENT_ID` | OIDC client identifier | `acs-backend` |
| `OIDC_CLIENT_SECRET` | OIDC client secret | (required for production) |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/acs` |
| `DB_USERNAME` | Database user | `acs` |
| `DB_PASSWORD` | Database password | `acs` |

## Packaging

```shell
./mvnw package
```

Produces `target/quarkus-app/quarkus-run.jar`. Run with:
```shell
java -jar target/quarkus-app/quarkus-run.jar
```

### Docker Image
```shell
./mvnw package -Dquarkus.container-image.build=true
```

### Native Executable
```shell
./mvnw package -Dnative
```

## Helm Deployment

The Helm chart is located at `src/main/helm/acs-backend/`.

```shell
helm install acs-backend src/main/helm/acs-backend/ \
  --set oidc.authServerUrl=http://keycloak:8080/realms/acs \
  --set postgresql.host=postgresql-rw \
  --set postgresql.database=acs
```

Features:
- Init containers for PostgreSQL readiness checks
- OIDC configuration via ConfigMaps and Secrets
- Optional PEM key mounting for JWT signing
- HPA auto-scaling support
- Ingress and Gateway API HTTPRoute support

## API Endpoints

| Method | Path | Description |
|:---|:---|:---|
| `POST` | `/api/auth/login` | Authenticate user |
| `POST` | `/api/auth/logout` | Invalidate session |
| `GET` | `/api/auth/me` | Current user profile |
| `GET` | `/api/auth/config` | OIDC configuration |
| `GET` | `/api/auth/providers` | Available identity providers |
| `GET` | `/api/auth/personas` | Available personas |
| `GET` | `/api/storage/requests` | List access requests |
| `POST` | `/api/storage/requests` | Submit access requests |
| `POST` | `/api/storage/requests/{id}/approve` | Approve request |
| `POST` | `/api/storage/requests/{id}/reject` | Reject request |
| `GET` | `/api/catalogs` | List registered catalogs |
| `GET` | `/api/audit/log` | View audit trail |
| `GET` | `/health` | Health check |

## Related Guides

- [Quarkus OIDC](https://quarkus.io/guides/security-oidc-code-flow-authentication)
- [SmallRye JWT](https://quarkus.io/guides/security-jwt)
- [SmallRye Health](https://quarkus.io/guides/smallrye-health)
- [SmallRye OpenAPI](https://quarkus.io/guides/openapi-swaggerui)
- [Hibernate ORM with Panache](https://quarkus.io/guides/hibernate-orm-panache)
