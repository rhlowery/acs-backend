package com.rhlowery.acs.domain;

import java.util.List;
import java.util.Map;

/**
 * Represents an access request in the system.
 * 
 * @param id Unique identifier for the request
 * @param requesterId The ID of the user who submitted the request
 * @param userId The ID of the principal the access is for
 * @param principalType The type of principal (e.g., USER, SERVICE_PRINCIPAL)
 * @param catalogName The name of the target catalog
 * @param schemaName The name of the target schema
 * @param tableName The name of the target table or resource
 * @param resourceType The type of resource (e.g., TABLE, VOLUME, MODEL)
 * @param privileges The list of privileges being requested (e.g., SELECT, READ)
 * @param status The current status of the request (e.g., PENDING, APPROVED)
 */
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
