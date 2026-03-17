package com.rhlowery.acs.service.impl;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UnityCatalogNodeProvider extends AbstractMockProvider {
    public UnityCatalogNodeProvider() {
        super("uc-oss", "UnityCatalogNodeProvider");
    }
}
