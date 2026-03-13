package com.rhlowery.acs.service.impl;

import com.rhlowery.acs.domain.CatalogNode;
import com.rhlowery.acs.domain.NodeType;
import com.rhlowery.acs.service.CatalogProvider;
import java.util.List;
import java.util.ArrayList;

public abstract class AbstractMockProvider implements CatalogProvider {
    protected final String catalogId;
    protected final String implClass;

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
        if ("/".equals(path)) {
            nodes.add(new CatalogNode("main", NodeType.CATALOG, "/main", implClass));
            nodes.add(new CatalogNode("default", NodeType.DATABASE, "/default", implClass));
        } else if (path.contains("default")) {
            nodes.add(new CatalogNode("sensitive_tbl", NodeType.TABLE, path + "/sensitive_tbl", implClass));
        } else if (path.contains("finance")) {
            nodes.add(new CatalogNode("quarters", NodeType.NAMESPACE, path + "/quarters", implClass));
        }
        return nodes;
    }

    @Override
    public void applyPolicy(String path, String action, String principal) {
        // Mock success
    }

    @Override
    public String getEffectivePermissions(String path, String principal) {
        if (path.contains("sensitive") || path.contains("salaries") || path.contains("users")) {
            if ("alice".equals(principal)) return "SELECT";
            if ("bob".equals(principal)) return "NONE";
            if ("charlie".equals(principal)) return "READ";
        }
        return "READ";
    }
}
