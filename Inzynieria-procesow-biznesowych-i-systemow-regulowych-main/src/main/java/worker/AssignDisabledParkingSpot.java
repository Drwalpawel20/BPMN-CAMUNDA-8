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
import java.util.HashMap;
import java.util.Map;

@Component
public class AssignDisabledParkingSpot {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignDisabledParkingSpot.class);

    private final DatabaseService databaseService;

    public AssignDisabledParkingSpot(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @JobWorker(type = "assign-disabled-parking-permit")
    public void assignSpot(final JobClient client, final ActivatedJob job) {

        LOGGER.info("=== AssignDisabledParkingPermitWorker STARTED ===");

        Map<String, Object> variables = job.getVariablesAsMap();

        String firstName = (String) variables.get("first_name");
        String lastName  = (String) variables.get("last_name");
        String index     = (String) variables.get("index");

        Map<String, Object> result = new HashMap<>();
        result.put("spot_assigned", false);
        result.put("remaining_free_spots", null);

        try (Connection conn = databaseService.getConnection()) {

            // 1. Pobranie liczby wolnych miejsc
            String selectSql =
                    "SELECT free_spots FROM parking.parking_spots WHERE category = ?";
            int freeSpots = 0;

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, "disabled");
                var rs = selectStmt.executeQuery();
                if (rs.next()) {
                    freeSpots = rs.getInt("free_spots");
                }
            }

            // 2. Jeśli są wolne miejsca → aktualizacja + INSERT
            if (freeSpots > 0) {

                // zmniejszenie liczby miejsc
                String updateSql =
                        "UPDATE parking.parking_spots " +
                                "SET free_spots = free_spots - 1 " +
                                "WHERE category = ?";

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, "disabled");
                    updateStmt.executeUpdate();
                }

                // INSERT do assigned_spots
                String insertSql =
                        "INSERT INTO parking.assigned_spots " +
                                "(first_name, last_name, person_id) " +
                                "VALUES (?, ?, ?)";

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, firstName);
                    insertStmt.setString(2, lastName);
                    insertStmt.setString(3, index);

                    insertStmt.executeUpdate();
                }

                result.put("spot_assigned", true);
                result.put("remaining_free_spots", freeSpots - 1);

                LOGGER.info(
                        "Parking spot assigned to {} {} (index={}). Remaining: {}",
                        firstName, lastName, index, freeSpots - 1
                );

            } else {
                LOGGER.info("No free disabled parking spots available");
                result.put("remaining_free_spots", 0);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to assign disabled parking spot", e);
        }

        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send();

        LOGGER.info("=== AssignDisabledParkingPermitWorker FINISHED ===");
    }
}
