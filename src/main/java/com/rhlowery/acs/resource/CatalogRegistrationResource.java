package com.rhlowery.acs.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

/**
 * REST Resource for dynamic catalog registration and management.
 * Allows on-the-fly onboarding of new catalog instances by providing connection settings
 * and metadata without requiring a service restart.
 */
@jakarta.enterprise.context.ApplicationScoped
@Path("/api/catalog/registrations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Catalog Registration", description = "Endpoints for managing dynamic catalog connections")
public class CatalogRegistrationResource {

    private static final Logger LOG = Logger.getLogger(CatalogRegistrationResource.class);
    private final Map<String, Map<String, Object>> registrations = new ConcurrentHashMap<>();

    @POST
    @Operation(summary = "Register a new catalog", description = "Adds a new catalog connection to the system")
    @APIResponse(responseCode = "201", description = "Catalog registered successfully")
    public Response registerCatalog(Map<String, Object> registration) {
        String id = (String) registration.get("id");
        if (id == null) return Response.status(400).entity(Map.of("error", "Missing id")).build();
        LOG.infof("Registering catalog: %s", id);
        registrations.put(id, new HashMap<>(registration));
        return Response.status(201).entity(registration).build();
    }

    @GET
    @Operation(summary = "List registered catalogs", description = "Returns a list of all currently registered catalog connections")
    public Response listRegistrations() {
        return Response.ok(new ArrayList<>(registrations.values())).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get catalog details", description = "Returns details for a specific catalog registration")
    @APIResponse(responseCode = "200", description = "Found")
    @APIResponse(responseCode = "404", description = "Not Found")
    public Response getRegistration(@PathParam("id") String id) {
        Map<String, Object> registration = registrations.get(id);
        if (registration == null) return Response.status(404).build();
        return Response.ok(registration).build();
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update catalog settings", description = "Partially updates the settings for an existing catalog registration")
    public Response updateRegistration(@PathParam("id") String id, Map<String, Object> update) {
        Map<String, Object> existing = registrations.get(id);
        if (existing == null) return Response.status(404).build();
        
        LOG.infof("Updating catalog: %s with: %s", id, update);
        
        // Ensure we are working with a mutable copy
        Map<String, Object> mutableExisting = new HashMap<>(existing);
        
        Object settingsObj = update.get("settings");
        if (settingsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> newSettings = (Map<String, Object>) settingsObj;
            
            Object existingSettingsObj = mutableExisting.get("settings");
            Map<String, Object> existingSettings;
            if (existingSettingsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>) existingSettingsObj;
                existingSettings = new HashMap<>(casted);
            } else {
                existingSettings = new HashMap<>();
            }
            
            existingSettings.putAll(newSettings);
            mutableExisting.put("settings", existingSettings);
        }
        
        // Update other top-level fields
        update.forEach((k, v) -> {
            if (!"settings".equals(k)) {
                mutableExisting.put(k, v);
            }
        });
        
        registrations.put(id, mutableExisting);
        return Response.ok(mutableExisting).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Unregister a catalog", description = "Removes a catalog connection from the system")
    @APIResponse(responseCode = "204", description = "Deleted")
    public Response deleteRegistration(@PathParam("id") String id) {
        if (registrations.remove(id) == null) return Response.status(404).build();
        return Response.noContent().build();
    }
}
