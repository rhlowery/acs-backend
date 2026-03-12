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
