package com.rhlowery.acs.infrastructure;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.util.Map;
import java.util.List;

@Mock
@ApplicationScoped
@RestClient
public class MockDatabricksClient implements DatabricksClient {

    @Override
    public Map<String, Object> fetchFromUC(String target, String token, Integer maxResults, String pageToken, String catalogName, String schemaName) {
        return Map.of("tables", List.of(Map.of("name", "sensitive_table")));
    }

    @Override
    public Map<String, Object> executeSql(String token, com.rhlowery.acs.domain.SqlRequest body) {
        return Map.of("status", "success", "statement_id", "test-id");
    }
}
