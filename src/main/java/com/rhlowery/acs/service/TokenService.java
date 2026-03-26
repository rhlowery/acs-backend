package com.rhlowery.acs.service;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    public String generateToken(String userId, String userName, List<String> groups, String role, String persona) {
        Set<String> roles = new HashSet<>(groups != null ? groups : List.of());
        if (role != null) roles.add(role);
        if (persona != null) roles.add(persona);
        
        return Jwt.issuer(issuer)
                .upn(userId)
                .groups(roles)
                .claim("userName", userName)
                .claim("role", role)
                .claim("persona", persona != null ? persona : "NONE")
                .sign();
    }
}
