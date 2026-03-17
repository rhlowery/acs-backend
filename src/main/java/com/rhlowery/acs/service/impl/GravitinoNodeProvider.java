package com.rhlowery.acs.service.impl;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GravitinoNodeProvider extends AbstractMockProvider {
    public GravitinoNodeProvider() {
        super("gravitino", "GravitinoNodeProvider");
    }
}
