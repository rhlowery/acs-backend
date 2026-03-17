package com.rhlowery.acs.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record CatalogNode(
    String name,
    NodeType type,
    String path,
    String implementation,
    java.util.List<String> approvers,
    String owner
) {}
