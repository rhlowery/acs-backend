package com.rhlowery.acs.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@jakarta.enterprise.context.ApplicationScoped
@Path("/api/catalog")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Catalog", description = "Endpoints for traversing data catalogs and managing nodes")
public class CatalogResource {

    @jakarta.inject.Inject
    com.rhlowery.acs.service.CatalogService catalogService;

    @GET
    @Path("/{catalogId}/nodes")
    @Operation(summary = "Get children of a catalog node", description = "Returns a list of nodes under the given path for a specific catalog")
    @APIResponse(responseCode = "200", description = "List of children", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = List.class)))
    @APIResponse(responseCode = "404", description = "Catalog not found")
    public Response getNodes(@Parameter(description = "ID of the catalog to query", required = true) @PathParam("catalogId") String catalogId,
                             @Parameter(description = "Path to the node within the catalog", required = false) @QueryParam("path") String path) {
        try {
            // Since we need CatalogNode objects, and CatalogService is currently 
            // designed with basic orchestration, let's keep a way to find providers 
            // or update CatalogService to return Nodes.
            // For now, let's assume CatalogService should handle this too.
            // Actually, to avoid breaking too much, I'll keep the findProvider logic 
            // but move it to CatalogService or just use it from there if I make it public.
            // Let's just update CatalogService to include getNodes.
            return Response.ok(((com.rhlowery.acs.service.impl.DefaultCatalogService)catalogService).getNodes(catalogId, path)).build();
        } catch (Exception e) {
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/{catalogId}/nodes/verify")
    @Operation(summary = "Verify a catalog node", description = "Performs a simple mock verification of a given catalog node path.")
    @APIResponse(responseCode = "200", description = "Node verification successful")
    @APIResponse(responseCode = "404", description = "Catalog not found")
    public Response verifyNode(@Parameter(description = "ID of the catalog", required = true) @PathParam("catalogId") String catalogId,
                               @Parameter(description = "Path to the node to verify", required = true) @QueryParam("path") String path) {
        try {
            catalogService.getNodes(catalogId, path);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/{catalogId}/nodes/policy")
    @Operation(summary = "Apply policy to a node", description = "Applies a security or audit policy to the specified catalog node")
    @RequestBody(description = "Policy application request containing path, action, and principal", required = true,
                 content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = Map.class),
                                    examples = @org.eclipse.microprofile.openapi.annotations.media.ExampleObject(value = "{\"path\": \"/data/table\", \"action\": \"READ\", \"principal\": \"user1\"}")))
    @APIResponse(responseCode = "202", description = "Policy application accepted")
    @APIResponse(responseCode = "404", description = "Catalog not found")
    public Response applyPolicy(@Parameter(description = "ID of the catalog", required = true) @PathParam("catalogId") String catalogId,
                                Map<String, String> request) {

        String path = request.get("path");
        String action = request.get("action");
        String principal = request.get("principal");
        
        try {
            catalogService.applyPolicy(catalogId, path, action, principal);
            return Response.accepted().build();
        } catch (Exception e) {
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/{catalogId}/nodes/permissions")
    @Operation(summary = "Get effective permissions", description = "Calculates and returns the effective permissions for a principal on a node")
    @APIResponse(responseCode = "200", description = "Effective permissions for the node", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Map.class), examples = @org.eclipse.microprofile.openapi.annotations.media.ExampleObject(value = "{\"effective\": \"READ\"}")))
    @APIResponse(responseCode = "404", description = "Catalog not found")
    public Response getPermissions(@Parameter(description = "ID of the catalog", required = true) @PathParam("catalogId") String catalogId,
                                   @Parameter(description = "Path to the node", required = true) @QueryParam("path") String path,
                                   @Parameter(description = "Principal for whom to get permissions", required = false) @QueryParam("principal") String principal) {
        try {
            String perm = catalogService.getEffectivePermissions(catalogId, path, principal != null ? principal : "admin");
            return Response.ok(Map.of("effective", perm)).build();
        } catch (Exception e) {
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        }
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
        return Response.ok(catalogService.listProviders()).build();
    }

}
