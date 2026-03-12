package com.rhlowery.acs.domain;

import java.util.List;
import java.util.Map;

public record AccessRequest(
    String id,
    String requesterId,
    String userId,
    String catalogName,
    String schemaName,
    String tableName,
    List<String> privileges,
    String status, // PENDING, APPROVED, REJECTED
    Long createdAt,
    Long updatedAt,
    String justification,
    List<String> approverGroups,
    Map<String, Object> metadata
) {}
