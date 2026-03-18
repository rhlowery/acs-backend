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
                nodes.add(new CatalogNode("polaris_cat", NodeType.CATALOG, "/polaris_cat", implClass, java.util.List.of("polaris-admins"), "admin", "Polaris catalog", "FULL_ACCESS"));
            } else if ("datahub".equals(catalogId)) {
                nodes.add(new CatalogNode("default", NodeType.DATABASE, "/default", implClass, java.util.List.of("datahub-admins"), "admin", "DataHub database", "FULL_ACCESS"));
            } else if ("gravitino".equals(catalogId)) {
                nodes.add(new CatalogNode("metalake", NodeType.NAMESPACE, "/metalake", implClass, java.util.List.of("gravitino-admins"), "admin", "Gravitino metalake", "FULL_ACCESS"));
            } else if ("atlan".equals(catalogId)) {
                nodes.add(new CatalogNode("asset", NodeType.TABLE, "/asset", implClass, java.util.List.of("atlan-admins"), "admin", "Atlan asset", "FULL_ACCESS"));
            } else if ("hive".equals(catalogId)) {
                nodes.add(new CatalogNode("hms_cat", NodeType.CATALOG, "/hms_cat", implClass, java.util.List.of("hms-admins"), "admin", "Hive Metastore", "FULL_ACCESS"));
            } else {
                nodes.add(new CatalogNode("main", NodeType.CATALOG, "/main", implClass, java.util.List.of("admins"), "admin", "Main catalog", "FULL_ACCESS"));
                nodes.add(new CatalogNode("default", NodeType.DATABASE, "/default", implClass, java.util.List.of("admins"), "admin", "Default database", "FULL_ACCESS"));
            }
        } else if ("/main".equals(path)) {
            nodes.add(new CatalogNode("default", NodeType.DATABASE, "/main/default", implClass, java.util.List.of("admins"), "admin", "Default database in main", "FULL_ACCESS"));
        } else if (path.contains("default")) {
            nodes.add(new CatalogNode("sensitive_tbl", NodeType.TABLE, path + "/sensitive_tbl", implClass, java.util.List.of("sensitive-approvers"), "data-governor", "Highly sensitive data", "READ_ONLY"));
        } else if (path.contains("finance")) {
            nodes.add(new CatalogNode("quarters", NodeType.NAMESPACE, path + "/quarters", implClass, java.util.List.of("finance-approvers"), "finance-lead", "Financial reports", "NO_ACCESS"));
        }
        return nodes;
    }
    @Override
    public CatalogNode getNode(String path) {
        if (path == null || "/".equals(path)) return new CatalogNode("root", NodeType.NAMESPACE, "/", implClass, java.util.List.of("admins"), "admin", "Root node", "ADMIN");
        if (path.contains("sensitive")) return new CatalogNode("sensitive_tbl", NodeType.TABLE, path, implClass, java.util.List.of("sensitive-approvers"), "data-governor", "Sensitive table", "READ");
        if (path.contains("salaries")) return new CatalogNode("salaries", NodeType.TABLE, path, implClass, java.util.List.of("finance-leads"), "finance_lead", "Salary info", "NONE");
        if (path.contains("finance")) return new CatalogNode("finance", NodeType.NAMESPACE, path, implClass, java.util.List.of("finance-approvers"), "finance_lead", "Finance dept", "READ");
        if (path.contains("staged")) return new CatalogNode("data_uploads", NodeType.VOLUME, path, implClass, java.util.List.of("data-governors"), "admin", "Staging area", "FULL");
        if (path.contains("model")) return new CatalogNode("risk-model", NodeType.MODEL, path, implClass, java.util.List.of("model-owners"), "admin", "ML Risk Model", "ACCESS");
        if (path.contains("compute")) return new CatalogNode("cluster-01", NodeType.COMPUTE, path, implClass, java.util.List.of("platform-admins"), "admin", "Databricks Cluster", "USAGE");
        if (path.contains("default")) return new CatalogNode("default", NodeType.DATABASE, path, implClass, java.util.List.of("default-admins"), "admin", "Default DB", "OWNER");
        return new CatalogNode("node", NodeType.TABLE, path, implClass, java.util.List.of("admins"), "admin", "Generic node", "READ");
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

        if (path != null && (path.contains("salaries") || path.contains("users") || path.contains("polaris"))) {
            if ("alice".equals(principal)) return "SELECT";
            if ("polaris".equals(principal)) return "READ";
            if ("bob".equals(principal)) return "NONE";
            if ("charlie".equals(principal)) return "READ";
        }
        if (path != null && path.contains("sensitive")) {
            if ("alice".equals(principal)) return "READ";
            if ("bob".equals(principal)) return "NONE";
            if ("charlie".equals(principal)) return "READ";
        }
        return "READ";
    }

    public void clear() {
        policies.clear();
    }
}
