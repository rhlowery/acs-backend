package com.rhlowery.acs.domain;

import java.util.Map;

public record AuditEntry(
    String id,
    String type,
    String actor,
    String userId,
    Long timestamp,
    Long serverTimestamp,
    Map<String, Object> details,
    String signature,
    String signer
) {}
