package com.rhlowery.acs.infrastructure.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "acs_audit_log")
public class AuditEntryEntity extends PanacheEntityBase {
    @Id
    public String id;
    public String type;
    public String actor;
    public String userId;
    public Long timestamp;
    public Long serverTimestamp;
    public String details; // Simple JSON string representation
    public String signature;
    public String signer;

    public AuditEntryEntity() {}

    public AuditEntryEntity(String id, String type, String actor, String userId, Long timestamp, 
                            Long serverTimestamp, String details, String signature, String signer) {
        this.id = id;
        this.type = type;
        this.actor = actor;
        this.userId = userId;
        this.timestamp = timestamp;
        this.serverTimestamp = serverTimestamp;
        this.details = details;
        this.signature = signature;
        this.signer = signer;
    }
}

