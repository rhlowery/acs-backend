# Access Control Service - Architecture Documentation

## Overview
The Access Control Service (ACS) is a centralized security and governance middleware designed to manage access policies across heterogeneous data catalogs (Databricks Unity Catalog, Hive Metastore, Glue, etc.) and identity providers (Keycloak, Okta, Azure AD).

## System Context (C4 Level 1)
```mermaid
graph TD
    User(["👤 Data Analyst / Engineer"])
    Admin(["🔧 Platform Admin"])
    ACS["🛡️ Access Control Service"]
    IdP["🔑 Identity Provider<br/>Keycloak / OIDC"]
    UC["📦 Unity Catalog"]
    HMS["🗄️ Hive Metastore"]
    DB[("🗃️ PostgreSQL")]
    OTel["📊 OpenTelemetry Collector"]

    User -->|"Authenticates"| IdP
    Admin -->|"Manages Personas & Policies"| ACS
    User -->|"Requests Access"| ACS
    ACS -->|"Validates Identity (JWKS)"| IdP
    ACS -->|"Discovers Metadata"| UC
    ACS -->|"Discovers Metadata"| HMS
    ACS -->|"Orchestrates Policies"| UC
    ACS -->|"Stores Requests / Audit / Users"| DB
    ACS -.->|"Emits Traces"| OTel
```

## Container Architecture (C4 Level 2)
```mermaid
graph LR
    subgraph frontend ["Frontend (React)"]
      UI["React SPA"]
    end

    subgraph acs ["ACS Backend (Quarkus)"]
      direction TB
      API["REST API Layer<br/>(JAX-RS Resources)"]
      Auth["AuthResource<br/>/api/auth/*"]
      Req["AccessRequestResource<br/>/api/storage/requests/*"]
      Cat["CatalogResource<br/>/api/catalogs/*"]
      Aud["AuditResource<br/>/api/audit/*"]
      Aug["SecurityIdentityAugmentor<br/>(Persona Resolution)"]
      Svc["Business Services<br/>(UserService, AccessRequestService)"]
      JPA["JPA / Panache<br/>(Entities)"]
    end

    UI -->|"REST / bff_jwt Cookie"| API
    API --- Auth
    API --- Req
    API --- Cat
    API --- Aud
    Auth --> Aug
    Aug --> Svc
    Req --> Svc
    Cat --> Svc
    Aud --> Svc
    Svc --> JPA
    JPA --> DB[("PostgreSQL")]
    Svc -.->|"RestClient"| ExtCat["External Catalogs<br/>(UC, HMS)"]
```

## Component Diagram (C4 Level 3)
```mermaid
graph TB
    subgraph resources ["JAX-RS Resources"]
        AuthRes["AuthResource"]
        AccReqRes["AccessRequestResource"]
        CatRes["CatalogResource"]
        AudRes["AuditResource"]
        UserRes["UserResource"]
        MetaRes["MetastoreResource"]
        ProxyRes["ProxyResource"]
    end

    subgraph services ["Business Services"]
        UserSvc["UserService"]
        AccReqSvc["AccessRequestService"]
        CatSvc["CatalogService"]
        TokSvc["TokenService"]
    end

    subgraph infrastructure ["Infrastructure"]
        Augmentor["AcsSecurityIdentityAugmentor"]
        Lineage["LineageService"]
        Health["HealthCheck"]
        MockIdP["MockIdentityProvider"]
    end

    subgraph persistence ["JPA Entities"]
        UserEnt["UserEntity"]
        GroupEnt["GroupEntity"]
        AccReqEnt["AccessRequestEntity"]
        AuditEnt["AuditEntryEntity"]
    end

    AuthRes --> TokSvc
    AuthRes --> UserSvc
    AccReqRes --> AccReqSvc
    AccReqRes --> CatSvc
    AccReqRes --> Lineage
    CatRes --> CatSvc
    AudRes --> AccReqSvc
    UserRes --> UserSvc
    Augmentor --> UserSvc

    UserSvc --> UserEnt
    UserSvc --> GroupEnt
    AccReqSvc --> AccReqEnt
```

## Authentication Flow (OIDC + BFF)
ACS uses a Backend-for-Frontend (BFF) pattern with dynamic OIDC discovery for production environments, while maintaining a Mock IdP for testing.

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant B as Backend (ACS)
    participant K as Keycloak (OIDC)
    participant A as Augmentor
    participant DB as PostgreSQL

    U->>F: Access UI
    F->>B: GET /api/auth/config
    B-->>F: OIDC Server URL + Client ID
    F->>K: Redirect to Login
    K-->>F: Authorization Code
    F->>B: POST /api/auth/login (code or credentials)
    B->>K: Exchange Code for Token (via OIDC)
    K-->>B: ID Token / Access Token
    B->>A: Augment SecurityIdentity
    A->>DB: Lookup User & Group personas
    DB-->>A: Persona = GOVERNANCE_ADMIN
    A-->>B: Enriched Identity (roles + persona)
    B->>B: Create BFF JWT (bff_jwt cookie)
    B-->>F: 200 OK + Set-Cookie
    F->>B: GET /api/auth/me (with cookie)
    B-->>F: User profile + persona
```

## Persona Resolution Strategy (UML Activity)
```mermaid
flowchart TD
    Start([Request Arrives]) --> Auth{Authenticated?}
    Auth -->|No| Anon[Return Anonymous]
    Auth -->|Yes| P1{DB User has persona?}
    P1 -->|Yes| UseDB["Use DB Persona"]
    P1 -->|No| P2{Group has persona mapping?}
    P2 -->|Yes| UseGroup["Use Group Persona"]
    P2 -->|No| P3{JWT has persona claim?}
    P3 -->|Yes| UseJWT["Use JWT Persona"]
    P3 -->|No| Default["No Persona (standard user)"]

    UseDB --> Enrich
    UseGroup --> Enrich
    UseJWT --> Enrich
    Default --> Enrich

    Enrich["Add persona + roles to SecurityIdentity"]
    Enrich --> AdminCheck{Persona == REQUESTER?}
    AdminCheck -->|Yes| SkipAdmin["Skip admin role promotion"]
    AdminCheck -->|No| PromoteAdmin{Has admin group/persona?}
    PromoteAdmin -->|Yes| AddAdmin["Add ADMIN, SECURITY_ADMIN, AUDITOR roles"]
    PromoteAdmin -->|No| SkipAdmin
    SkipAdmin --> Done([Return Augmented Identity])
    AddAdmin --> Done
```

## Domain Model (UML Class Diagram)
```mermaid
classDiagram
    class User {
        +String id
        +String name
        +String email
        +String role
        +String persona
        +List~String~ groups
    }
    class Group {
        +String id
        +String name
        +String description
        +String persona
    }
    class AccessRequest {
        +String id
        +String requesterId
        +String userId
        +String catalogName
        +String schemaName
        +String tableName
        +String status
        +String justification
        +String rejectionReason
        +List~String~ privileges
        +List~String~ approverGroups
        +Map metadata
        +Long expirationTime
    }
    class AuditEntry {
        +String id
        +String actor
        +String type
        +String details
        +long timestamp
    }
    class CatalogNode {
        +String id
        +String name
        +String type
        +String path
        +String permissions
    }

    User "1" --> "*" AccessRequest : creates
    User "*" --> "*" Group : belongs to
    Group "1" --> "*" User : contains
    AccessRequest "*" --> "1" CatalogNode : targets
    AccessRequest "*" --> "*" Group : requires approval from
    AuditEntry "*" --> "1" User : involves
```

## Persona-Based RBAC Model
```mermaid
graph LR
    subgraph Personas
        REQ["REQUESTER<br/>Can submit requests"]
        APP["APPROVER<br/>Can approve/reject"]
        GOV["GOVERNANCE_ADMIN<br/>Full governance control"]
        SEC["SECURITY_ADMIN<br/>Full security control"]
        AUD["AUDITOR<br/>Read-only audit access"]
        ADM["ADMIN<br/>Full system access"]
    end

    REQ -->|"Cannot"| Approve["Approve/Reject Requests"]
    APP -->|"Can"| Approve
    GOV -->|"Can"| Approve
    SEC -->|"Can"| Approve
    ADM -->|"Can"| Approve
    AUD -->|"Can view"| Audit["Audit Logs"]
```

## Configuration
Authentication is configured via environment variables:

| Variable | Description | Example |
|:---|:---|:---|
| `OIDC_AUTH_SERVER_URL` | The discovery endpoint for the OIDC IdP | `http://keycloak:8080/realms/acs` |
| `OIDC_CLIENT_ID` | The registered OIDC client ID | `acs-backend` |
| `OIDC_CLIENT_SECRET` | The OIDC client secret | (from K8s Secret) |
| `DB_URL` | PostgreSQL JDBC connection string | `jdbc:postgresql://pg:5432/acs` |
| `DB_USERNAME` | Database user | `acs` |
| `DB_PASSWORD` | Database password | (from K8s Secret) |

## Deployment
Managed via Helm (`src/main/helm/acs-backend`). The chart includes:
- **Init containers** for database readiness (`waitOnStart`).
- **ConfigMaps** for environment-specific OIDC and catalog settings.
- **Secrets** for DB password, OIDC client secret, and optional PEM keys.
- **HPA** integration for horizontal auto-scaling.
- **Ingress** and **Gateway API HTTPRoute** support.
- **Resource limits** (256Mi–512Mi memory, 100m–500m CPU).
