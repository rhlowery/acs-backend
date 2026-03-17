package com.rhlowery.acs.service.impl;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AtlanNodeProvider extends AbstractMockProvider {
    public AtlanNodeProvider() {
        super("atlan", "AtlanNodeProvider");
    }
}
