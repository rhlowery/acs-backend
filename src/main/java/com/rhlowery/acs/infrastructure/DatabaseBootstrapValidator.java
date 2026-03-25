package com.rhlowery.acs.infrastructure;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.jboss.logging.Logger;

/**
 * Validates database connectivity on startup with clear error messages
 * to distinguish between network and authentication issues.
 */
@ApplicationScoped
public class DatabaseBootstrapValidator {
    private static final Logger LOG = Logger.getLogger(DatabaseBootstrapValidator.class);

    @Inject
    DataSource dataSource;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Validating database connection on bootstrap...");
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(5)) {
                LOG.info("Successfully connected to the database.");
            }
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            String message = e.getMessage().toLowerCase();

            LOG.error("---------------------------------------------------------");
            LOG.error("DATABASE BOOTSTRAP FAILURE");
            
            if (sqlState != null && (sqlState.startsWith("28") || message.contains("password authentication failed") || message.contains("invalid authorization"))) {
                LOG.error("CAUSE: Authentication Failure (Invalid username or password)");
                LOG.error("HINT: Check DB_USERNAME and DB_PASSWORD environment variables.");
            } else if (message.contains("connection refused") || message.contains("timeout") || message.contains("unknown host") || message.contains("unreachable")) {
                LOG.error("CAUSE: Network Connectivity / Timeout");
                LOG.error("HINT: Check DB_URL and ensure the database host is reachable from this container.");
            } else {
                LOG.error("CAUSE: Unexpected SQL Error [" + sqlState + "]");
                LOG.error("DETAILS: " + e.getMessage());
            }
            LOG.error("---------------------------------------------------------");
            
            // We don't necessarily throw an exception here as Quarkus might handle retries 
            // or the health check will capture it, but the explicit log helps troubleshooting.
        } catch (Exception e) {
            LOG.error("Critical error during database validation: " + e.getMessage(), e);
        }
    }
}
