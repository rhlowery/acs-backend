package com.rhlowery.acs.infrastructure;

import com.rhlowery.acs.domain.User;
import com.rhlowery.acs.service.UserService;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.smallrye.mutiny.Uni;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Augments the authenticated SecurityIdentity with ACS-specific personas and groups.
 * This ensures that whether the user logs in via the BFF (JWT) or directly via OIDC,
 * the persona mapping logic remains consistent.
 */
@ApplicationScoped
public class AcsSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

    @Inject
    UserService userService;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }

        return Uni.createFrom().item(identity).onItem().transform(id -> {
            String userId = id.getPrincipal().getName();
            Set<String> roles = id.getRoles();
            
            // 1. Resolve Persona (Database -> Group -> Token -> Default)
            String persona = null;
            
            // Priority 1: User-specific persona in local database
            Optional<User> localUser = userService.getUser(userId);
            if (localUser.isPresent() && localUser.get().persona() != null) {
                persona = localUser.get().persona();
            }
            
            // Priority 2: Group-based persona resolution
            if (persona == null && !roles.isEmpty()) {
                for (String groupId : roles) {
                    Optional<com.rhlowery.acs.domain.Group> g = userService.getGroup(groupId);
                    if (g.isPresent() && g.get().persona() != null) {
                        persona = g.get().persona();
                        break;
                    }
                }
            }
            
            // Priority 3: Persona defined in JWT token (OIDC provider)
            if (persona == null && identity.getPrincipal() instanceof org.eclipse.microprofile.jwt.JsonWebToken) {
                org.eclipse.microprofile.jwt.JsonWebToken jwt = (org.eclipse.microprofile.jwt.JsonWebToken) identity.getPrincipal();
                persona = jwt.getClaim("persona");
            }
            
            // 2. Provision user if not exists (JIT)
            if (localUser.isEmpty()) {
                User newUser = new User(userId, userId, userId + "@example.com", "STANDARD_USER", List.copyOf(roles), persona);
                userService.saveUser(newUser);
            }

            // 3. Build augmented identity
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(id);
            
            // Collect all roles to add
            Set<String> rolesToApply = new HashSet<>(roles);
            
            if (localUser.isPresent() && localUser.get().groups() != null) {
                rolesToApply.addAll(localUser.get().groups());
            }

            if (persona != null) {
                rolesToApply.add(persona);
                builder.addAttribute("persona", persona);
            }
            
            // 4. Ensure admin personas get corresponding roles, unless explicitly restricted to REQUESTER
            if (!"REQUESTER".equals(persona)) {
                if (rolesToApply.contains("admins") || "ADMIN".equals(persona) || "GOVERNANCE_ADMIN".equals(persona) || "SECURITY_ADMIN".equals(persona)) {
                    rolesToApply.add("ADMIN");
                    rolesToApply.add("SECURITY_ADMIN");
                    rolesToApply.add("AUDITOR");
                }
            }

            for (String role : rolesToApply) {
                builder.addRole(role);
            }

            return (SecurityIdentity) builder.build();
        }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }
}
