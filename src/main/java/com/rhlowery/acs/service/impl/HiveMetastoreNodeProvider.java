package com.rhlowery.acs.service.impl;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HiveMetastoreNodeProvider extends AbstractMockProvider {
    public HiveMetastoreNodeProvider() {
        super("hive", "HiveMetastoreNodeProvider");
    }
}
