package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.service.IdentityProvider;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

/**
 * Mock Identity Provider for local development and demo purposes.
 * Ships with a default platform administrator (admin/admin).
 * Additional users can register themselves via the register() method.
 */
@ApplicationScoped
public class MockIdentityProvider implements IdentityProvider {

    private static final Logger LOG = Logger.getLogger(MockIdentityProvider.class);

    /** userId -> MockUser record */
    private final Map<String, MockUser> users = new ConcurrentHashMap<>();

    record MockUser(String userId, String password, String name, String email, String persona, List<String> groups) {}

    public MockIdentityProvider() {
        // Seed default platform administrator
        users.put("admin", new MockUser(
            "admin", "admin", "Platform Administrator", "admin@localhost",
            "PLATFORM_ADMIN", List.of("admins", "governance-team")
        ));
    }

    @Override
    public String getId() {
        return "mock";
    }

    @Override
    public String getName() {
        return "Mock Identity Provider";
    }

    @Override
    public String getType() {
        return "MOCK";
    }

    @Override
    public Optional<Map<String, Object>> authenticate(Map<String, Object> credentials) {
        String userId = (String) credentials.get("userId");
        String password = (String) credentials.get("password");

        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        MockUser user = users.get(userId);
        if (user != null) {
            // Existing user — validate password
            if (password == null || !password.equals(user.password())) {
                LOG.warn("Mock IdP: invalid password for user '" + userId + "'");
                return Optional.empty();
            }
            
            // Update groups on every login to reflect the current test scenario
            @SuppressWarnings("unchecked")
            List<String> updatedGroups = (List<String>) credentials.get("groups");
            if (updatedGroups != null) {
                user = new MockUser(user.userId(), user.password(), user.name(), user.email(), (String)credentials.get("persona"), updatedGroups);
                users.put(userId, user);
                LOG.infof("Mock IdP: authenticated existing user '%s' with updated groups %s", userId, updatedGroups);
            }
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("userId", user.userId());
            result.put("userName", user.name());
            result.put("email", user.email());
            result.put("providerId", getId());
            result.put("groups", user.groups());
            if (user.persona() != null) {
                result.put("persona", user.persona());
            }
            return Optional.of(result);
        }

        // Unknown user — auto-register with the supplied password, groups, and persona (dev/test convenience)
        if (password != null && !password.isBlank()) {
            @SuppressWarnings("unchecked")
            List<String> groups = (List<String>) credentials.get("groups");
            if (groups == null) groups = List.of();
            
            String persona = (String) credentials.get("persona");

            MockUser newUser = new MockUser(userId, password, userId, userId + "@mock.local", persona, groups);
            users.put(userId, newUser);
            LOG.infof("Mock IdP: auto-registered new user '%s' with groups %s and persona %s", userId, groups, persona);
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("userId", userId);
            result.put("userName", userId);
            result.put("email", newUser.email());
            result.put("providerId", getId());
            result.put("groups", groups);
            if (persona != null) {
                result.put("persona", persona);
            }
            return Optional.of(result);
        }

        LOG.warn("Mock IdP: unknown user '" + userId + "' and no password provided");
        return Optional.empty();
    }

    @Override
    public List<String> getGroups(String userId) {
        MockUser user = users.get(userId);
        return user != null ? user.groups() : List.of();
    }

    /**
     * Programmatic registration for tests or runtime use.
     */
    public void register(String userId, String password, String name, String persona, List<String> groups) {
        users.put(userId, new MockUser(userId, password, name, userId + "@mock.local", persona, groups));
    }

    /**
     * Check if a given userId exists in the mock store.
     */
    public boolean hasUser(String userId) {
        return users.containsKey(userId);
    }
}
