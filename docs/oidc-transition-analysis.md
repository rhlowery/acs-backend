# OIDC Integration & Architectural Transition Analysis

## Objective
To enable seamless integration with standard OIDC providers (e.g., Keycloak) and expose authentication flows within OpenAPI 3.0 via Swagger UI.

## 1. OpenAPI & Swagger UI Setup
By adding the `OPENIDCONNECT` security scheme to the ACS Backend, Swagger UI will natively support the "Authorize" button, prompting for OIDC flow redirects.
- **Security Scheme**: `oidc`
- **Type**: `OPENIDCONNECT`
- **Discovery**: Dynamic via `${OIDC_AUTH_SERVER_URL}/.well-known/openid-configuration`.

## 2. Dialect Support (e.g. Keycloak)
The Quarkus OIDC extension (`quarkus-oidc`) provides specialized dialect support for several major IdPs:
- **`quarkus.oidc.type=keycloak`**: Optimizes for Keycloak's JWKS and introspection endpoints.
- **`quarkus.oidc.type=azure`**: Adapts to Azure AD's multi-tenant discovery patterns.
This ensures ACS can correctly decode and validate tokens produced by these providers without manual certificate management.

## 3. Evaluation: SmallRye JWT vs. Quarkus OIDC

| Feature | SmallRye JWT (Current) | Quarkus OIDC (Proposed) |
| :--- | :--- | :--- |
| **Trust Source** | Local PEM files (Static) | JWKS Discovery (Dynamic) |
| **Integration** | Manual BFF signing/validation | Automatic IdP orchestration |
| **Session Mgmt** | Custom Cookie (`bff_jwt`) | Standard Session Cookie (`q_session`) |
| **State** | Stateless / Local | Stateful (Web App) or Stateless (Bearer) |

### Impact of Transition
1. **AuthResource.java**:
   - REDUCED: The login POST endpoint is mostly superseded by the OIDC redirect flow.
   - MODIFIED: Custom provisioning and persona mapping must move to a `SecurityIdentityAugmentor`.
   - RETAINED: Profile `/me` and persona management APIs.
2. **Infrastructure**:
   - REMOVED: Static PEM files (`privateKey.pem`, `publicKey.pem`) from the classpath.
   - ADDED: `quarkus.oidc` configuration in `application.properties`.
3. **Test Suite**:
   - BREAKING: `RestAssured` tests currently simulate login by POSTing to `/api/auth/login` and getting a local token.
   - REQUIREMENT: Must adopt `io.quarkus.test.oidc.server.OidcWiremockTestResource` to mock OIDC behavior or switch to `@TestSecurity`.

## 4. Effort Estimation

| Task | Estimated Effort |
| :--- | :--- |
| Security Refactoring (Augmentors, Config) | 12 hours |
| AuthResource Logic Migration | 8 hours |
| Test Suite Adaptation (160+ scenarios) | 24 hours |
| **Total** | **44 hours (~6 days)** |

## Recommendation
Transitioning to `quarkus-oidc` is **recommended** for production parity and security standardization. The effort is primarily concentrated in the test suite adaptation. The immediate first step—exposing the OIDC scheme in OpenAPI—has been implemented to allow documentation-driven integration.
