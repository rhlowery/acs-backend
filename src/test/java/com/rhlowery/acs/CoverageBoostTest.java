package com.rhlowery.acs;

import com.rhlowery.acs.domain.AccessRequest;
import com.rhlowery.acs.domain.AuditEntry;
import com.rhlowery.acs.domain.Persona;
import com.rhlowery.acs.domain.User;
import com.rhlowery.acs.resource.AuditResource;
import com.rhlowery.acs.resource.CatalogRegistrationResource;
import com.rhlowery.acs.resource.MetastoreResource;
import com.rhlowery.acs.service.UserService;
import com.rhlowery.acs.service.AccessRequestService;
import com.rhlowery.acs.service.AuditService;
import com.rhlowery.acs.service.CatalogService;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CoverageBoostTest {

    @Inject
    AccessRequestService accessRequestService;

    @Inject
    AuditService auditService;

    @Inject
    UserService userService;

    @Inject
    CatalogService catalogService;


    @Inject
    CatalogRegistrationResource registrationResource;

    @Inject
    MetastoreResource metastoreResource;

    @Inject
    AuditResource auditResource;

    @Test
    public void boostAccessRequestService() {
        String id = UUID.randomUUID().toString();
        AccessRequest req = new AccessRequest(
            id, "alice", "alice", "USER", "uc-oss", "default", "tbl", "TABLE", 
            List.of("READ"), "PENDING", System.currentTimeMillis(), null, "Test", null, 
            List.of("admins"), Map.of(), null
        );
        
        accessRequestService.saveRequests(List.of(req), "alice", List.of("users"), false);
        
        AccessRequest update = new AccessRequest(
            id, null, null, null, null, null, null, null, null, "APPROVED", null, null, null, null, null, null, null
        );
        assertThrows(RuntimeException.class, () -> 
            accessRequestService.saveRequests(List.of(update), "eve", List.of("other-group"), false)
        );
        
        accessRequestService.saveRequests(List.of(update), "eve", List.of("admins"), false);
        assertEquals("APPROVED", accessRequestService.getRequestById(id).get().status());
        
        AccessRequest partial = new AccessRequest(
            id, null, null, null, null, null, null, null, null, "PENDING", null, null, null, null, null, null, null
        );
        accessRequestService.saveRequests(List.of(partial), "alice", List.of("admins"), true);
        
        assertTrue(accessRequestService.getAllRequests("admin", List.of("admins"), true).size() > 0);
        assertTrue(accessRequestService.getAllRequests("alice", List.of("users"), false).size() > 0);
        
        AccessRequest userReq = new AccessRequest(UUID.randomUUID().toString(), "admin", "target-user", "USER", "cat", "sch", "tbl", "TABLE", List.of("READ"), "PENDING", 0L, 0L, "J", null, null, null, null);
        accessRequestService.saveRequests(List.of(userReq), "admin", List.of("admins"), true);
        assertTrue(accessRequestService.getAllRequests("target-user", List.of(), false).size() > 0);

        // Branch: r.approverGroups() != null
        AccessRequest groupReq = new AccessRequest(UUID.randomUUID().toString(), "alice", "alice", "USER", "cat", "sch", "tbl", "TABLE", List.of("READ"), "PENDING", 0L, 0L, "J", null, List.of("group1"), null, null);
        accessRequestService.saveRequests(List.of(groupReq), "admin", List.of("admins"), true);
        assertTrue(accessRequestService.getAllRequests("user2", List.of("group1"), false).size() > 0);
    }

    @Test
    public void boostAuditService() {
        for (int i = 0; i < 50; i++) { // Limit for DB persistence speed
            auditService.log(new AuditEntry(UUID.randomUUID().toString(), "TYPE", "actor", "user", 
                System.currentTimeMillis(), System.currentTimeMillis(), Map.of(), "SIG", "ORIGIN"));
        }
        assertTrue(auditService.getLogs().size() >= 50);
        assertNotNull(auditService.streamLogs());
    }


    @Test
    public void boostUserService() {
        Optional<User> alice = userService.getUser("alice");
        assertTrue(alice.isPresent());
        
        userService.updateUserGroups("alice", List.of("admins", "users"));
        assertTrue(userService.getUser("alice").get().groups().contains("admins"));
        
        userService.updateUserPersona("alice", "AUDITOR");
        assertEquals("AUDITOR", userService.getUser("alice").get().persona());

        userService.updateGroupPersona("admins", "ADMIN");
        assertEquals("ADMIN", userService.getGroup("admins").get().persona());

        assertThrows(IllegalArgumentException.class, () -> userService.updateUserGroups("non-existent", List.of("group")));
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserPersona("non-existent", "ADMIN"));
        assertThrows(IllegalArgumentException.class, () -> userService.updateGroupPersona("non-existent", "ADMIN"));
        
        assertTrue(userService.listUsers().size() > 0);
        assertTrue(userService.listGroups().size() > 0);
    }

    @Test
    public void boostCatalogService() {
        assertNotNull(catalogService.getNodes("uc-oss", "/"));
        assertThrows(RuntimeException.class, () -> catalogService.getNodes("non-existent", "/"));
        assertThrows(RuntimeException.class, () -> catalogService.getEffectivePermissions("non-existent", "/", "user"));
        assertThrows(RuntimeException.class, () -> catalogService.applyPolicy("non-existent", "/", "READ", "user"));
        assertTrue(catalogService.getRequiredApprovers("non-existent", "/").isEmpty());
        assertNotNull(catalogService.getNodes("uc-oss", "/main/default"));
        assertNotNull(catalogService.getRequiredApprovers("uc-oss", "/default/sensitive_tbl"));
        assertNotNull(catalogService.getRequiredApprovers("uc-oss", "/main/default/some_table"));
        catalogService.getRequiredApprovers("uc-oss", "");
        catalogService.getRequiredApprovers("uc-oss", "/");
        catalogService.verifyPolicy("uc-oss", "/main", "READ", "principal");
        catalogService.verifyPolicy("non-existent", "/main", "READ", "principal");
        catalogService.listProviders();
        catalogService.getProviders();
        catalogService.clear();
    }

    @Test
    public void boostCatalogRegistrationResource() {
        String id = "test-cat";
        Map<String, Object> reg = new HashMap<>(Map.of("id", id, "name", "Test"));
        registrationResource.registerCatalog(reg);
        registrationResource.registerCatalog(Map.of()); // Missing id branch
        
        assertEquals(200, registrationResource.getRegistration(id).getStatus());
        assertEquals(404, registrationResource.getRegistration("ghost").getStatus());
        
        registrationResource.updateRegistration(id, Map.of("settings", Map.of("key", "val"), "other", "val"));
        registrationResource.updateRegistration(id, Map.of("settings", "NOT_A_MAP"));
        registrationResource.updateRegistration("ghost", Map.of());
        
        String id2 = "test-cat-2";
        registrationResource.registerCatalog(Map.of("id", id2));
        registrationResource.updateRegistration(id2, Map.of("settings", Map.of("key", "val")));

        registrationResource.listRegistrations();
        registrationResource.deleteRegistration(id);
        registrationResource.deleteRegistration("ghost");
    }

    @Test
    public void boostMetastoreResource() {
        metastoreResource.getChildren("uc-oss", "/", 1, null);
        metastoreResource.getChildren("uc-oss", "/non-existent", 1, null);
        metastoreResource.getChildren("uc-oss", "/", 1, "invalid");
        metastoreResource.getChildren("uc-oss", "/", 2, null);
    }

    @Test
    public void boostAuditResource() {
        auditResource.getLog();
        auditResource.logUi(Map.of("level", "INFO", "message", "Test"));
    }

    @Test
    public void boostPersona() {
        assertNotNull(Persona.all());
    }
}
