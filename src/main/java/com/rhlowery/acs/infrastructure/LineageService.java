package com.rhlowery.acs.infrastructure;

import io.openlineage.client.OpenLineage;
import io.openlineage.client.OpenLineageClient;
import io.openlineage.client.transports.ConsoleTransport;
import io.openlineage.client.transports.Transport;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import com.rhlowery.acs.domain.AccessRequest;

@ApplicationScoped
public class LineageService {

    private final OpenLineage ol = new OpenLineage(URI.create("https://github.com/rhlowery/acs-backend/tag/1.0.0"));
    private final OpenLineageClient client;

    public LineageService() {
        Transport transport = new ConsoleTransport();
        this.client = OpenLineageClient.builder().transport(transport).build();
    }

    public void emitAccessRequestEvent(AccessRequest request, String type) {
        UUID runId = UUID.randomUUID();
        ZonedDateTime now = ZonedDateTime.now();

        OpenLineage.RunEvent.EventType eventType;
        switch (type) {
            case "SUBMITTED":
                eventType = OpenLineage.RunEvent.EventType.START;
                break;
            case "APPROVED":
                eventType = OpenLineage.RunEvent.EventType.COMPLETE;
                break;
            case "REJECTED":
                eventType = OpenLineage.RunEvent.EventType.FAIL;
                break;
            default:
                eventType = OpenLineage.RunEvent.EventType.OTHER;
        }

        OpenLineage.RunEvent runEvent = ol.newRunEventBuilder()
            .eventTime(now)
            .eventType(eventType)
            .run(ol.newRunBuilder()
                .runId(runId)
                .facets(ol.newRunFacetsBuilder().build())
                .build())
            .job(ol.newJobBuilder()
                .namespace("acs-backend")
                .name("process-access-request")
                .facets(ol.newJobFacetsBuilder().build())
                .build())
            .inputs(List.of(
                ol.newInputDatasetBuilder()
                    .namespace("unity-catalog")
                    .name(request.catalogName() + "." + request.schemaName() + "." + request.tableName())
                    .facets(ol.newDatasetFacetsBuilder().build())
                    .build()
            ))
            .outputs(List.of(
                ol.newOutputDatasetBuilder()
                    .namespace("acs-permissions")
                    .name(request.userId() + ":" + request.status())
                    .facets(ol.newDatasetFacetsBuilder().build())
                    .build()
            ))
            .build();

        client.emit(runEvent);
    }
}
