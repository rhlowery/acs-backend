package com.rhlowery.acs.service.impl;


import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AzureAdIdentityProvider extends AbstractMockIdp {
    public AzureAdIdentityProvider() {
        super("azure-ad", "Microsoft Entra ID", "OIDC");
    }
}
