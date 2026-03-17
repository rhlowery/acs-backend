package com.rhlowery.acs.service.impl;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DatabricksNodeProvider extends AbstractMockProvider {
    public DatabricksNodeProvider() {
        super("databricks", "DatabricksNodeProvider");
    }
}
