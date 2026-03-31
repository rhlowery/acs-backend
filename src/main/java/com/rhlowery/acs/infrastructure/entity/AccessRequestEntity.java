package com.rhlowery.acs.infrastructure.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;

@Entity
@Table(name = "acs_access_requests")
public class AccessRequestEntity extends PanacheEntityBase {
    @Id
    public String id;
    public String requesterId;
    public String userId;
    public String principalType;
    public String catalogName;
    public String schemaName;
    public String tableName;
    public String resourceType;
    @ElementCollection(fetch = FetchType.EAGER)
    public List<String> privileges;
    public String status;
    public long createdAt;
    public Long updatedAt;
    public String justification;
    public String rejectionReason;
    @ElementCollection(fetch = FetchType.EAGER)
    public List<String> approverGroups;
    public Long expirationTime;

    public AccessRequestEntity() {}

    public AccessRequestEntity(String id, String requesterId, String userId, String principalType,
                               String catalogName, String schemaName, String tableName, String resourceType,
                               List<String> privileges, String status, long createdAt, Long updatedAt,
                               String justification, String rejectionReason, List<String> approverGroups,
                               Long expirationTime) {
        this.id = id;
        this.requesterId = requesterId;
        this.userId = userId;
        this.principalType = principalType;
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.resourceType = resourceType;
        this.privileges = privileges;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.justification = justification;
        this.rejectionReason = rejectionReason;
        this.approverGroups = approverGroups;
        this.expirationTime = expirationTime;
    }
}
