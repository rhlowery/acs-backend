package com.rhlowery.acs.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for 3rd-party identity providers.
 */
public interface IdentityProvider {
    /**
     * @return Unique identifier for this provider.
     */
    String getId();

    /**
     * @return Human-readable name of the provider.
     */
    String getName();

    /**
     * @return The type of provider (e.g., OIDC, SAML, ActiveDirectory).
     */
    String getType();

    /**
     * Authenticates a user against the 3rd-party provider.
     * @param credentials Map of credentials (e.g., username/password or external token).
     * @return Map containing user information if successful.
     */
    Optional<Map<String, Object>> authenticate(Map<String, Object> credentials);

    /**
     * Retrieves group memberships for a user from the provider.
     * @param userId The unique identifier of the user.
     * @return List of groups the user belongs to.
     */
    List<String> getGroups(String userId);

    /**
     * Resets the provider state (for tests).
     */
    default void clear() {}
}
