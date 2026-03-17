package com.rhlowery.acs.service.impl;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OktaIdentityProvider extends AbstractMockIdp {
    public OktaIdentityProvider() {
        super("okta", "Okta", "OIDC");
    }

    @Override
    public java.util.List<String> getGroups(String userId) {
        if ("admin".equals(userId)) {
            return java.util.List.of("admins", "okta-admins");
        }
        return super.getGroups(userId);
    }
}
