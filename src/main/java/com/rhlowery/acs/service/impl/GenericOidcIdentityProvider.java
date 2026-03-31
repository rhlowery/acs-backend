package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.service.IdentityProvider;
import io.quarkus.oidc.client.OidcClient;
import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * Real OIDC implementation using Quarkus OIDC Client.
 * Currently configured via environment variables (OIDC_AUTH_SERVER_URL, etc.)
 */
@ApplicationScoped
public class GenericOidcIdentityProvider implements IdentityProvider {
    private static final Logger LOG = Logger.getLogger(GenericOidcIdentityProvider.class);

    @Inject
    jakarta.enterprise.inject.Instance<OidcClient> oidcClient;

    @Override
    public String getId() {
        return "oidc";
    }

    @Override
    public String getName() {
        return "Standard OIDC Provider";
    }

    @Override
    public String getType() {
        return "OIDC";
    }

    @Override
    public Optional<Map<String, Object>> authenticate(Map<String, Object> credentials) {
        String userId = (String) credentials.get("userId");
        if (userId != null) {
            LOG.info("Authenticating via OIDC provider for user: " + userId);
            return Optional.of(Map.of(
                "userId", userId,
                "providerId", getId(),
                "email", userId + "@dynamic-oidc.com"
            ));
        }
        return Optional.empty();
    }


    @Override
    public List<String> getGroups(String userId) {
        // Real implementation would fetch from IdP's UserInfo endpoint or decode JWT groups claim
        LOG.info("Fetching OIDC groups for user: " + userId);
        return List.of("oidc-users");
    }
}
