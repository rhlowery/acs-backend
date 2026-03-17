# ACS Backend (Access Request Service)

Welcome to the documentation for the ACS Backend. This service provides a HATEOAS-compliant REST API for managing access requests, integrated with Unity Catalog.

## Features

*   **RESTful API**: Secure, HATEOAS-compliant resources.
*   **Security**: JWT-based authentication and authorization.
*   **Generic Catalog Interface**: Pluggable architecture for traversing Unity Catalog, Glue, Iceberg, Polaris, Data Hub, Gravitino, Atlan, and Hive Metastore.
*   **Dynamic Catalog Registration**: In addition to static provider discovery via ServiceLoader, the system supports dynamic registration of catalog connections through the `/api/catalog/registrations` API. Once registered, a catalog's resources can be managed specifically via its ID:
*   **Isolated Resources**: Manage a catalog's nodes, policies, and permissions using `/api/catalog/{id}/nodes`. This clarifies whether you are interacting with the registered provider itself or a resource under it.
*   **On-the-fly onboarding**: Registering new catalog instances without restarting the service.
*   **Observability**: Integrated OpenTelemetry for distributed tracing and OpenLineage for data flow tracking.
*   **Integrations**: Proxies to Databricks and Unity Catalog APIs.
*   **Audit**: Complete audit logging of all access-related activities.

## Navigation

Use the menu on the left to explore the architecture, Javadocs, and source code cross-references.
