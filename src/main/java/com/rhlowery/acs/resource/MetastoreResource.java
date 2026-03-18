package com.rhlowery.acs.resource;

import com.rhlowery.acs.domain.CatalogNode;
import com.rhlowery.acs.service.CatalogService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@jakarta.enterprise.context.ApplicationScoped
@Path("/api/metastores")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Metastores", description = "Metadata discovery for data sources")
public class MetastoreResource {

    private static final Logger LOG = Logger.getLogger(MetastoreResource.class);

    @Inject
    CatalogService catalogService;

    @GET
    @Path("/{sourceId}/children")
    @Operation(summary = "Get metastore children", description = "Returns a flat list of child nodes for a given path in a metastore")
    public Response getChildren(
            @PathParam("sourceId") String sourceId,
            @QueryParam("path") @DefaultValue("/") String path,
            @QueryParam("depth") @DefaultValue("1") int depth,
            @QueryParam("page_token") String pageToken) {
        
        LOG.infof("Metastore children request: sourceId=%s, path=%s, depth=%d, pageToken=%s", 
                sourceId, path, depth, pageToken);
        
        try {
            List<CatalogNode> results = new ArrayList<>();
            fetchRecursive(sourceId, path, depth, results);
            
            // Basic pagination logic
            int offset = 0;
            if (pageToken != null && !pageToken.isEmpty()) {
                try {
                    offset = Integer.parseInt(pageToken);
                } catch (NumberFormatException e) {
                    return Response.status(400).entity(Map.of("error", "Invalid page token")).build();
                }
            }
            
            int limit = 100;
            int total = results.size();
            int end = Math.min(offset + limit, total);
            
            List<CatalogNode> page = results.subList(offset, end);
            String nextToken = end < total ? String.valueOf(end) : null;
            
            return Response.ok(Map.of(
                "nodes", page,
                "next_page_token", nextToken != null ? nextToken : ""
            )).build();
            
        } catch (Exception e) {
            LOG.error("Error fetching metastore children", e);
            return Response.status(404).entity(Map.of("error", e.getMessage())).build();
        }
    }

    private void fetchRecursive(String sourceId, String path, int depth, List<CatalogNode> results) {
        if (depth <= 0) return;
        
        List<CatalogNode> children = catalogService.getNodes(sourceId, path);
        results.addAll(children);
        
        if (depth > 1) {
            for (CatalogNode child : children) {
                fetchRecursive(sourceId, child.path(), depth - 1, results);
            }
        }
    }
}
