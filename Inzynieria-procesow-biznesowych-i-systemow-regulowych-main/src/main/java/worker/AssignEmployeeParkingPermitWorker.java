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
public class AssignEmployeeParkingPermitWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AssignEmployeeParkingPermitWorker.class);

    private final DatabaseService databaseService;

    public AssignEmployeeParkingPermitWorker(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @JobWorker(type = "assign-employee-parking-permit")
    public void assignSpot(final JobClient client, final ActivatedJob job) {

        LOGGER.info("=== AssignEmployeeParkingPermitWorker STARTED ===");

        Map<String, Object> variables = job.getVariablesAsMap();

        String firstName = (String) variables.get("first_name");
        String lastName  = (String) variables.get("last_name");
        String index     = (String) variables.get("index");

        Map<String, Object> result = new HashMap<>();
        result.put("spot_assigned", false);
        result.put("remaining_free_spots", null);

        try (Connection conn = databaseService.getConnection()) {

            conn.setAutoCommit(false);

            // 0. Sprawdzenie czy osoba już ma miejsce
            String existsSql =
                    "SELECT 1 FROM parking.assigned_spots WHERE person_id = ?";

            try (PreparedStatement ps = conn.prepareStatement(existsSql)) {
                ps.setString(1, index);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    LOGGER.info("Person {} already has assigned parking spot", index);
                    conn.rollback();
                    complete(client, job, result);
                    return;
                }
            }

            // 1. Pobranie liczby wolnych miejsc
            String selectSql =
                    "SELECT free_spots FROM parking.parking_spots WHERE category = ?";

            int freeSpots = 0;

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, "normal");
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    freeSpots = rs.getInt("free_spots");
                }
            }

            if (freeSpots > 0) {

                // 2. UPDATE parking_spots
                String updateSql =
                        "UPDATE parking.parking_spots " +
                                "SET free_spots = free_spots - 1 " +
                                "WHERE category = ?";

                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, "normal");
                    ps.executeUpdate();
                }

                // 3. INSERT assigned_spots
                String insertSql =
                        "INSERT INTO parking.assigned_spots " +
                                "(first_name, last_name, person_id) " +
                                "VALUES (?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, firstName);
                    ps.setString(2, lastName);
                    ps.setString(3, index);
                    ps.executeUpdate();
                }

                conn.commit();

                result.put("spot_assigned", true);
                result.put("remaining_free_spots", freeSpots - 1);

                LOGGER.info(
                        "Employee parking spot assigned to {} {} (person_id={}). Remaining: {}",
                        firstName, lastName, index, freeSpots - 1
                );

            } else {
                conn.rollback();
                LOGGER.info("No free parking spots available in category 'normal'");
                result.put("remaining_free_spots", 0);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to assign employee parking spot", e);
        }

        complete(client, job, result);

        LOGGER.info("=== AssignEmployeeParkingPermitWorker FINISHED ===");
    }

    private void complete(JobClient client,
                          ActivatedJob job,
                          Map<String, Object> vars) {

        client.newCompleteCommand(job.getKey())
                .variables(vars)
                .send();
    }
}
