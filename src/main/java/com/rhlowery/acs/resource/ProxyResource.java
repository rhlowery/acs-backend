package com.rhlowery.acs.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@jakarta.enterprise.context.ApplicationScoped
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Proxy", description = "Discovery and SQL proxy endpoints for Databricks and Unity Catalog")
public class ProxyResource {

    private static final Logger LOG = Logger.getLogger(ProxyResource.class);

    @jakarta.inject.Inject
    @org.eclipse.microprofile.rest.client.inject.RestClient
    com.rhlowery.acs.infrastructure.DatabricksClient databricksClient;

    @GET
    @Path("/sdk/{target}")
    @Operation(summary = "Proxy SDK fetch", description = "Proxies a fetch request to the Databricks Unity Catalog SDK placeholder")
    public Response sdkFetch(@PathParam("target") String target, 
                            @HeaderParam("x-workspace-host") String host,
                            @HeaderParam("Authorization") String auth) {
        LOG.infof("SDK Fetch: target=%s host=%s", target, host);
        try {
            Map<String, Object> response = databricksClient.fetchFromUC(target, auth, 10, null, null, null);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("SDK Fetch error", e);
            return Response.status(500).entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/uc/{path: .*}")
    @Operation(summary = "Proxy Unity Catalog request", description = "Proxies a generic request to the Unity Catalog API placeholder")
    public Response ucProxy(@PathParam("path") String path) {
        LOG.infof("UC Proxy: path=%s", path);
        return Response.status(501).entity(Map.of("error", "Not implemented", "path", path)).build();
    }

    @POST
    @Path("/sql/execute")
    @Operation(summary = "Execute SQL", description = "Proxies a SQL execution request to the Databricks SQL Warehouse placeholder")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response executeSql(Map<String, Object> request, @HeaderParam("Authorization") String auth) {
        String statement = (String) request.get("statement");
        if (statement == null || statement.trim().isEmpty()) {
            return Response.status(400).entity(Map.of("error", "Empty statement")).build();
        }
        LOG.infof("SQL Execute: %s", statement);
        try {
            com.rhlowery.acs.domain.SqlRequest sqlReq = new com.rhlowery.acs.domain.SqlRequest(statement, null);
            Map<String, Object> response = databricksClient.executeSql(auth, sqlReq);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("SQL Execute error", e);
            return Response.status(500).entity(Map.of("error", e.getMessage())).build();
        }
    }
}
