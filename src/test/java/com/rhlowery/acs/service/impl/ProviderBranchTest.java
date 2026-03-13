package com.rhlowery.acs.service.impl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ProviderBranchTest {

    @Test
    public void testAbstractProviderBranches() {
        UnityCatalogNodeProvider provider = new UnityCatalogNodeProvider();
        
        // getChildren branches
        assertFalse(provider.getChildren("/").isEmpty());
        assertFalse(provider.getChildren("/default").isEmpty());
        assertFalse(provider.getChildren("/finance").isEmpty());
        assertTrue(provider.getChildren("/other").isEmpty());

        // getEffectivePermissions branches
        assertEquals("SELECT", provider.getEffectivePermissions("/sensitive", "alice"));
        assertEquals("NONE", provider.getEffectivePermissions("/salaries", "bob"));
        assertEquals("READ", provider.getEffectivePermissions("/users", "charlie"));
        assertEquals("READ", provider.getEffectivePermissions("/other", "alice"));
        assertEquals("READ", provider.getEffectivePermissions("/sensitive", "other"));
    }
}
