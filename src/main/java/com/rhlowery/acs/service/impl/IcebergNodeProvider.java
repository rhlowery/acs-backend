package com.rhlowery.acs.service.impl;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IcebergNodeProvider extends AbstractMockProvider {
    public IcebergNodeProvider() {
        super("iceberg", "IcebergNodeProvider");
    }
}
