package worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import service.DatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@Component
public class CheckExistingAplications {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CheckExistingAplications.class);

    private final DatabaseService databaseService;

    public CheckExistingAplications(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @JobWorker(type = "check-existing-applications")
    public void checkExisting(final JobClient client, final ActivatedJob job) {

        LOGGER.info("=== CheckExistingApplicationsWorker STARTED ===");

        Map<String, Object> variables = job.getVariablesAsMap();

        String firstName = (String) variables.get("first_name");
        String lastName  = (String) variables.get("last_name");
        String index     = (String) variables.get("index");

        boolean exists = false;

        try (Connection conn = databaseService.getConnection()) {

            // 1. Sprawdzenie assigned_spots
            String assignedSql =
                    "SELECT 1 FROM parking.assigned_spots " +
                            "WHERE first_name = ? AND last_name = ? AND person_id = ? " +
                            "LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(assignedSql)) {
                ps.setString(1, firstName);
                ps.setString(2, lastName);
                ps.setString(3, index);

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    exists = true;
                }
            }

            // 2. Jeśli nie znaleziono → sprawdzenie candidates
            if (!exists) {
                String candidatesSql =
                        "SELECT 1 FROM parking.candidates " +
                                "WHERE first_name = ? AND last_name = ? AND index_number = ? " +
                                "LIMIT 1";

                try (PreparedStatement ps = conn.prepareStatement(candidatesSql)) {
                    ps.setString(1, firstName);
                    ps.setString(2, lastName);
                    ps.setString(3, index);

                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        exists = true;
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to check existing applications", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("is_existing", exists);

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send();

        LOGGER.info("is_existing = {}", exists);
        LOGGER.info("=== CheckExistingApplicationsWorker FINISHED ===");
    }
}
