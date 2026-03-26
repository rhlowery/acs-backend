package com.rhlowery.acs.infrastructure;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * Global OpenAPI configuration for ACS Backend.
 * Defines the OIDC security scheme to be visible in Swagger UI.
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Access Control Service API",
        version = "1.0.0",
        description = "Centralized governance and access policy orchestration for Unity Catalog and heterogeneous data sources.",
        contact = @Contact(name = "Platform Security Team", email = "security@rhlowery.com"),
        license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")
    ),
    security = @SecurityRequirement(name = "oidc")
)
@SecurityScheme(
    securitySchemeName = "oidc",
    type = SecuritySchemeType.OPENIDCONNECT,
    description = "OpenID Connect authentication for ACS",
    openIdConnectUrl = "${OIDC_AUTH_SERVER_URL}/.well-known/openid-configuration"
)
public class OpenApiConfig extends Application {
}
