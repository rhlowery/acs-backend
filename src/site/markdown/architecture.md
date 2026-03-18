# Architecture Documentation

This page provides a high-level overview of the ACS Backend architecture using C4 and UML diagrams.

## System Context (C4)

The following diagram illustrates how the ACS Backend (BFF) orchestrates access requests across various identity and catalog providers.

```mermaid
graph TD
    subgraph "External Systems"
        IDP["Identity Providers (Okta/AzureAD)"]
        UC["Unity Catalog / Databricks"]
        Glue["AWS Glue / Lake Formation"]
        Other["Other Catalogs (Iceberg/Gravitino)"]
        Lineage["OpenLineage Consumer"]
        OTel["OTel Collector"]
    end

    subgraph "ACS Infrastructure"
        UI["ACS Frontend (Web UI)"]
        BFF["ACS Backend (BFF)"]
    end

    User["User (Consumer/Approver)"] -- "Interacts with" --> UI
    UI -- "REST / JWT" --> BFF
    BFF -- "Sign tokens as" --> IDP
    BFF -- "Syncs policies to" --> UC
    BFF -- "Syncs policies to" --> Glue
    BFF -- "Syncs policies to" --> Other
    BFF -- "Emits traces to" --> OTel
    BFF -- "Emits lineage to" --> Lineage
    BFF -- "Discovers metadata from" --> Metastores["Various Metastores (Unity, Polaris, etc.)"]
```

## Domain Model (UML)

The core domain entities managed by the ACS Backend for auditing, policy enforcement, and request lifecycle management.

```mermaid
classDiagram
    class User {
        +String id
        +String name
        +String email
        +String role
        +List~String~ groups
    }
    class Group {
        +String id
        +String name
        +String description
    }
    class AccessRequest {
        +String id
        +String requesterId
        +String userId
        +String principalType
        +String catalogName
        +String schemaName
        +String tableName
        +String resourceType
        +List~String~ privileges
        +String status
        +Long createdAt
        +Long updatedAt
        +String justification
        +String rejectionReason
        +List~String~ approverGroups
        +Map~String,Object~ metadata
    }
    class Persona {
        +String id
        +String name
        +String description
    }
    class CatalogNode {
        +String name
        +NodeType type
        +String path
        +List~String~ approvers
        +String owner
        +String comment
        +String permissions
    }
    class NodeType {
        <<enumeration>>
        CATALOG
        DATABASE
        SCHEMA
        TABLE
        VOLUME
        MODEL
        COMPUTE
        NAMESPACE
    }

    User "1" -- "*" AccessRequest : initiates/requests_for
    Group "*" -- "*" User : member_of
    AccessRequest "*" -- "1" CatalogNode : targets
    CatalogNode "1" -- "1" NodeType : is_of_type
    User "*" -- "1" Persona : assigned_persona
    Group "*" -- "1" Persona : assigned_persona
```

## Generic Catalog Interface

The ACS Backend provides a pluggable architecture for interacting with various data catalogs. This is achieved through the `CatalogProvider` SPI (Service Provider Interface).

### Key Components:

*   **CatalogProvider**: The interface that all catalog implementations must satisfy.
*   **ServiceLoader**: Used to dynamically discover and load available catalog providers at runtime.
*   **Common Domain**: Unified `CatalogNode` and `NodeType` definitions allow for a consistent experience across different catalog backends.

Available implementations include:
*   **UnityCatalogNodeProvider**
*   **GlueNodeProvider**
*   **DatabricksNodeProvider**
*   **IcebergNodeProvider**
*   **PolarisNodeProvider**
*   **DataHubNodeProvider**
*   **GravitinoNodeProvider**
*   **AtlanNodeProvider**
*   **HiveMetastoreNodeProvider**

## Metastore Discovery

The system provides an enhanced metadata discovery API (`/api/metastores/`) tailored for deep catalog exploration. Key features include:
*   **Recursive Fetching**: Support for a `depth` parameter to fetch children multiple levels deep in a single request.
*   **Fully-Qualified Paths**: All results are returned as a flat list of paths with associated metadata.
*   **Pagination**: Built-in support for paginating large nodes using `page_token` and `next_page_token`.

## Technical Stack

*   **Runtime**: Quarkus (Java 17)
*   **API**: REST with JAX-RS (RestEasy Reactive)
*   **Security**: SmallRye JWT
*   **Observability**: SmallRye Health & Micrometer Prometheus
*   **Testing**: JUnit 5, RestAssured, Cucumber

## Observability

The system implements advanced observability using two key frameworks:

### OpenTelemetry (OTel)
Used for distributed tracing. Every request to the BFF is automatically traced, providing visibility into the full request lifecycle from the frontend to backend and downstream proxies.

### OpenLineage
Used for tracking data and process flows. When an access request is submitted, approved, or rejected, a lineage event is emitted to capture the transition of metadata between the user and the Unity Catalog datasets.
