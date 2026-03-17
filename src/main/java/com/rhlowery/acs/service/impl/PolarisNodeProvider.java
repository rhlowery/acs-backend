package com.rhlowery.acs.service.impl;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PolarisNodeProvider extends AbstractMockProvider {
    public PolarisNodeProvider() {
        super("polaris", "PolarisNodeProvider");
    }
}
