package com.rhlowery.acs.service.impl;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GlueNodeProvider extends AbstractMockProvider {
    public GlueNodeProvider() {
        super("glue-prod", "GlueNodeProvider");
    }
}
