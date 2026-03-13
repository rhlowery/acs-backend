# Architecture Documentation

This page provides a high-level overview of the ACS Backend architecture using C4 and UML diagrams.

## System Context (C4)

The following diagram illustrates how the ACS Backend interacts with users and external systems.

![System Context](images/context.png)

## Domain Model (UML)

The core domain entities and their relationships are shown below.

![Class Diagram](images/classes.png)

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
