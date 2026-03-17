package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.service.IdentityProvider;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractMockIdp implements IdentityProvider {
    protected final String id;
    protected final String name;
    protected final String type;

    protected AbstractMockIdp(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Optional<Map<String, Object>> authenticate(Map<String, Object> credentials) {
        String userId = (String) credentials.get("userId");
        if (userId != null) {
            return Optional.of(Map.of(
                "userId", userId,
                "providerId", id,
                "email", userId + "@" + id + ".com"
            ));
        }
        return Optional.empty();
    }

    @Override
    public List<String> getGroups(String userId) {
        return List.of("external-users", id + "-users");
    }
}
