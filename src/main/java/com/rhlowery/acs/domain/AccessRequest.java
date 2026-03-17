package com.rhlowery.acs.domain;

import java.util.List;
import java.util.Map;

public record AccessRequest(
    String id,
    String requesterId,
    String userId,
    String principalType, // USER, SERVICE_PRINCIPAL, GROUP
    String catalogName,
    String schemaName,
    String tableName,
    String resourceType, // TABLE, VOLUME, MODEL
    List<String> privileges,
    String status, // PENDING, APPROVED, REJECTED, VERIFIED, PARTIALLY_APPROVED
    Long createdAt,
    Long updatedAt,
    String justification,
    String rejectionReason,
    List<String> approverGroups,
    Map<String, Object> metadata,
    Long expirationTime
) {}
