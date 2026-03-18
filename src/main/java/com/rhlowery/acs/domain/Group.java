package com.rhlowery.acs.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Represents a group in the system.
 * 
 * @param id Unique identifier for the group
 * @param name Display name of the group
 * @param description Brief description of the group's purpose
 * @param persona System-wide persona (Optional)
 */
@RegisterForReflection
public record Group(
    String id,
    String name,
    String description,
    String persona
) {}
