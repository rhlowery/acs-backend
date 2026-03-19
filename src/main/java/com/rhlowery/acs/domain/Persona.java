package com.rhlowery.acs.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Defines the available system-wide personas in the ACS.
 */
@RegisterForReflection
public record Persona(
    String id,
    String name,
    String description
) {
    public static final Persona ADMIN = new Persona("ADMIN", "Admin", "Full system access and configuration");
    public static final Persona APPROVER = new Persona("APPROVER", "Approver", "Can review and approve/reject requests");
    public static final Persona REQUESTER = new Persona("REQUESTER", "Requester", "Can submit and view own access requests");
    public static final Persona SECURITY_ADMIN = new Persona("SECURITY_ADMIN", "Security Admin", "System-wide security configuration and policy management");
    public static final Persona PLATFORM_ADMIN = new Persona("PLATFORM_ADMIN", "Platform Admin", "Infrastructure and platform level administration");
    public static final Persona GOVERNANCE_ADMIN = new Persona("GOVERNANCE_ADMIN", "Governance Admin", "Default data governance and catalog level administration");
    public static final Persona AUDITOR = new Persona("AUDITOR", "Auditor", "Can view and stream full audit logs for historical or real-time review");
    public static final Persona REVIEWER = new Persona("REVIEWER", "Reviewer", "Has read-only access to audit dashboards and live streams");

    public static java.util.List<Persona> all() {
        return java.util.List.of(ADMIN, APPROVER, REQUESTER, SECURITY_ADMIN, PLATFORM_ADMIN, GOVERNANCE_ADMIN, AUDITOR, REVIEWER);
    }
}
