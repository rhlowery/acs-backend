package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.CatalogNode;
import com.rhlowery.acs.domain.NodeType;
import com.rhlowery.acs.service.CatalogProvider;
import java.util.List;
import java.util.ArrayList;

public abstract class AbstractMockProvider implements CatalogProvider {
    protected final String catalogId;
    protected final String implClass;

    protected final java.util.Map<String, String> policies = new java.util.concurrent.ConcurrentHashMap<>();

    protected AbstractMockProvider(String catalogId, String implClass) {
        this.catalogId = catalogId;
        this.implClass = implClass;
    }

    @Override
    public String getCatalogId() {
        return catalogId;
    }

    @Override
    public List<CatalogNode> getChildren(String path) {
        List<CatalogNode> nodes = new ArrayList<>();
        if (path == null || "/".equals(path)) {
            if ("polaris".equals(catalogId)) {
                nodes.add(new CatalogNode("polaris_cat", NodeType.CATALOG, "/polaris_cat", implClass));
            } else if ("datahub".equals(catalogId)) {
                nodes.add(new CatalogNode("default", NodeType.DATABASE, "/default", implClass));
            } else if ("gravitino".equals(catalogId)) {
                nodes.add(new CatalogNode("metalake", NodeType.NAMESPACE, "/metalake", implClass));
            } else if ("atlan".equals(catalogId)) {
                nodes.add(new CatalogNode("asset", NodeType.TABLE, "/asset", implClass));
            } else if ("hive".equals(catalogId)) {
                nodes.add(new CatalogNode("hms_cat", NodeType.CATALOG, "/hms_cat", implClass));
            } else {
                nodes.add(new CatalogNode("main", NodeType.CATALOG, "/main", implClass));
                nodes.add(new CatalogNode("default", NodeType.DATABASE, "/default", implClass));
            }
        } else if (path.contains("default")) {
            nodes.add(new CatalogNode("sensitive_tbl", NodeType.TABLE, path + "/sensitive_tbl", implClass));
        } else if (path.contains("finance")) {
            nodes.add(new CatalogNode("quarters", NodeType.NAMESPACE, path + "/quarters", implClass));
        }
        return nodes;
    }

    @Override
    public void applyPolicy(String path, String action, String principal) {
        if ("overlay".equalsIgnoreCase(action) || "revoke".equalsIgnoreCase(action) || "approve".equalsIgnoreCase(action)) {
            return;
        }
        policies.put(principal + ":" + path, action);
    }

    @Override
    public String getEffectivePermissions(String path, String principal) {
        String action = policies.get(principal + ":" + path);
        if (action != null) return action;

        if (path != null && (path.contains("sensitive") || path.contains("salaries") || path.contains("users") || path.contains("polaris"))) {
            if ("alice".equals(principal)) return "SELECT";
            if ("polaris".equals(principal)) return "READ";
            if ("bob".equals(principal)) return "NONE";
            if ("charlie".equals(principal)) return "READ";
        }
        return "READ";
    }

    public void clear() {
        policies.clear();
    }
}
