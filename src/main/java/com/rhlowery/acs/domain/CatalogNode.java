package com.rhlowery.acs.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Represents a node in a data catalog (e.g., catalog, database, schema, table).
 * 
 * @param name The display name of the node
 * @param type The type of the node (CATALOG, DATABASE, etc.)
 * @param path The canonical path to the node in the catalog
 * @param implementation The class name of the provider that manages this node
 * @param approvers List of group IDs that can approve access to this node
 * @param owner The ID of the primary owner of this node
 */
@RegisterForReflection
public record CatalogNode(
    String name,
    NodeType type,
    String path,
    String implementation,
    java.util.List<String> approvers,
    String owner,
    String comment,
    String permissions
) {}
