package com.rhlowery.acs.service;

import java.util.List;

public interface CatalogService {
    void applyPolicy(String catalogId, String path, String action, String principal);
    String getEffectivePermissions(String catalogId, String path, String principal);
    List<String> listProviders();
    boolean verifyPolicy(String catalogId, String path, String expectedAction, String principal);
    List<com.rhlowery.acs.domain.CatalogNode> getNodes(String catalogId, String path);
    List<String> getProviders();
    List<String> getRequiredApprovers(String catalogId, String path);
    void clear();
}
