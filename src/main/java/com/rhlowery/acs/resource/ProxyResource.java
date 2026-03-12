package com.rhlowery.acs.resource;

import com.rhlowery.acs.infrastructure.DatabricksClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class ProxyResource {

    @Inject
    @RestClient
    DatabricksClient databricksClient;

    @GET
    @Path("/sdk/{target}")
    public Response sdkFetch(
        @PathParam("target") String target,
        @HeaderParam("x-workspace-host") String host,
        @QueryParam("catalog_name") String catalogName,
        @QueryParam("schema_name") String schemaName,
        @QueryParam("max_results") Integer maxResults,
        @QueryParam("page_token") String pageToken,
        @Context SecurityContext securityContext
    ) {
        String token = "Bearer mock-token"; // Resolve from session/cookies
        
        try {
            Map<String, Object> result = databricksClient.fetchFromUC(target, token, maxResults, pageToken, catalogName, schemaName);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/catalog/search")
    public Response searchCatalog(
        @QueryParam("query") String query,
        @HeaderParam("x-workspace-host") String host
    ) {
        String token = "Bearer mock-token";
        try {
            Map<String, Object> result = databricksClient.fetchFromUC("tables", token, 1000, null, null, null);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allTables = (List<Map<String, Object>>) result.getOrDefault("tables", List.of());
            
            List<Map<String, Object>> filtered = allTables.stream()
                .filter(t -> t.get("name").toString().toLowerCase().contains(query.toLowerCase()) ||
                             t.get("catalog_name").toString().toLowerCase().contains(query.toLowerCase()) ||
                             t.get("schema_name").toString().toLowerCase().contains(query.toLowerCase()))
                .limit(100)
                .collect(Collectors.toList());

            return Response.ok(Map.of("results", filtered)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", "Search failed: " + e.getMessage())).build();
        }
    }

    @POST
    @Path("/sql/execute")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response executeSql(Map<String, Object> body) {
        String token = "Bearer mock-token";
        try {
            Map<String, Object> result = databricksClient.executeSql(token, body);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/uc/{path: .*}")
    public Response ucProxy(@PathParam("path") String path, @HeaderParam("x-workspace-host") String host) {
        // Implement generic proxying if needed, or redirect to client
        return Response.status(Response.Status.NOT_IMPLEMENTED).entity(Map.of("error", "Generic UC proxying not fully implemented")).build();
    }
}
