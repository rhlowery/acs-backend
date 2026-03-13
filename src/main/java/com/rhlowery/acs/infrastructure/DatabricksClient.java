package com.rhlowery.acs.infrastructure;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.Map;

@RegisterRestClient(configKey = "databricks-api")
public interface DatabricksClient {

    @GET
    @Path("/api/2.1/unity-catalog/{target}")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> fetchFromUC(
        @PathParam("target") String target,
        @HeaderParam("Authorization") String token,
        @QueryParam("max_results") Integer maxResults,
        @QueryParam("page_token") String pageToken,
        @QueryParam("catalog_name") String catalogName,
        @QueryParam("schema_name") String schemaName
    );

    @POST
    @Path("/api/2.0/sql/statements")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> executeSql(
        @HeaderParam("Authorization") String token,
        com.rhlowery.acs.domain.SqlRequest body
    );
}
