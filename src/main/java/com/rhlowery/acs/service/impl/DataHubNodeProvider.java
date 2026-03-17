package com.rhlowery.acs.service.impl;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DataHubNodeProvider extends AbstractMockProvider {
    public DataHubNodeProvider() {
        super("datahub", "DataHubNodeProvider");
    }
}
