package com.rhlowery.acs.resource;

import com.rhlowery.acs.service.CatalogProvider;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

@jakarta.enterprise.context.ApplicationScoped
@Path("/api/catalog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogResource {

    private final List<CatalogProvider> providers;

    public CatalogResource() {
        this.providers = ServiceLoader.load(CatalogProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toList());
    }

    @GET
    @Path("/nodes")
    public Response getNodes(@QueryParam("catalogId") String catalogId, @QueryParam("path") String path) {
        CatalogProvider provider = findProvider(catalogId);
        if (provider == null) return Response.status(404).entity(Map.of("error", "Catalog not found")).build();
        return Response.ok(provider.getChildren(path)).build();
    }

    @GET
    @Path("/nodes/verify")
    public Response verifyNode(@QueryParam("path") String path) {
        // Simple mock verification
        return Response.ok().build();
    }

    @POST
    @Path("/nodes/policy")
    public Response applyPolicy(Map<String, String> request) {
        String path = request.get("path");
        String action = request.get("action");
        String principal = request.get("principal");
        
        providers.forEach(p -> p.applyPolicy(path, action, principal));
        return Response.accepted().build();
    }

    @GET
    @Path("/nodes/permissions")
    public Response getPermissions(@QueryParam("path") String path, @QueryParam("principal") String principal) {
        // Return first non-empty effective permission
        String perm = providers.stream()
            .map(p -> p.getEffectivePermissions(path, principal != null ? principal : "admin"))
            .filter(p -> !"NONE".equals(p))
            .findFirst()
            .orElse("NONE");
        return Response.ok(Map.of("effective", perm)).build();
    }

    @GET
    @Path("/search")
    public Response searchCatalog(@QueryParam("q") String q, @QueryParam("query") String query) {
        // Simulating search across providers
        List<Map<String, Object>> results = List.of(
            Map.of("name", "sensitive_table", "type", "TABLE")
        );
        return Response.ok(Map.of("results", results)).build();
    }

    @GET
    @Path("/providers")
    public Response listProviders() {
        List<String> classes = providers.stream()
            .map(p -> p.getClass().getName())
            .collect(Collectors.toList());
        return Response.ok(classes).build();
    }

    private CatalogProvider findProvider(String id) {
        if (id == null) return null;
        return providers.stream()
            .filter(p -> id.equals(p.getCatalogId()))
            .findFirst()
            .orElse(null);
    }
}
