package com.rhlowery.acs.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

/**
 * Represents a user in the system.
 * 
 * @param id Unique identifier (usually email or username)
 * @param name Full name of the user
 * @param email Email address
 * @param role The user's primary role (e.g., ADMIN, STANDARD_USER)
 * @param groups List of group IDs the user belongs to
 * @param persona System-wide persona (Optional)
 */
@RegisterForReflection
public record User(
    String id,
    String name,
    String email,
    String role, // ADMIN, STANDARD_USER
    List<String> groups,
    String persona
) {}
