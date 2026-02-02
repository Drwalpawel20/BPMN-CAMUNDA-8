package worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.api.response.ActivatedJob;
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
public class AssignFreeSpotsToCandidatesWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AssignFreeSpotsToCandidatesWorker.class);

    private final DatabaseService databaseService;

    public AssignFreeSpotsToCandidatesWorker(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @JobWorker(type = "assign-free-spots-to-candidates")
    public void handle(JobClient client, ActivatedJob job) {

        LOGGER.info("=== AssignFreeSpotsToCandidatesWorker STARTED ===");

        int assignedCount = 0;

        try (Connection conn = databaseService.getConnection()) {

            conn.setAutoCommit(false);

            int freeSpots = getFreeSpots(conn);
            LOGGER.info("Free spots available: {}", freeSpots);

            if (freeSpots <= 0) {
                conn.rollback();
                complete(client, job, 0);
                return;
            }

            String candidatesSql =
                    "SELECT c.first_name, c.last_name, c.index_number " +
                            "FROM parking.candidates c " +
                            "WHERE c.qualified = TRUE " +
                            "AND NOT EXISTS ( " +
                            "   SELECT 1 FROM parking.assigned_spots a " +
                            "   WHERE a.person_id = c.index_number " +
                            ") " +
                            "ORDER BY c.score ASC " +
                            "LIMIT ? " +
                            "FOR UPDATE";

            try (PreparedStatement ps = conn.prepareStatement(candidatesSql)) {
                ps.setInt(1, freeSpots);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {

                        String firstName = rs.getString("first_name");
                        String lastName  = rs.getString("last_name");
                        String index     = rs.getString("index_number");

                        String insertSql =
                                "INSERT INTO parking.assigned_spots " +
                                        "(first_name, last_name, person_id) " +
                                        "VALUES (?, ?, ?)";

                        try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                            ins.setString(1, firstName);
                            ins.setString(2, lastName);
                            ins.setString(3, index);
                            ins.executeUpdate();
                        }

                        assignedCount++;
                    }
                }
            }

            if (assignedCount > 0) {
                String updateSpotsSql =
                        "UPDATE parking.parking_spots " +
                                "SET free_spots = free_spots - ? " +
                                "WHERE category = 'normal'";

                try (PreparedStatement ps = conn.prepareStatement(updateSpotsSql)) {
                    ps.setInt(1, assignedCount);
                    ps.executeUpdate();
                }
            }

            conn.commit();

            LOGGER.info("Assigned {} parking spots", assignedCount);

        } catch (Exception e) {
            LOGGER.error("Failed to assign free spots", e);
        }

        complete(client, job, assignedCount);

        LOGGER.info("=== AssignFreeSpotsToCandidatesWorker FINISHED ===");
    }

    private int getFreeSpots(Connection conn) throws Exception {
        String sql =
                "SELECT free_spots " +
                        "FROM parking.parking_spots " +
                        "WHERE category = 'normal' " +
                        "FOR UPDATE";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("free_spots");
            }
        }
        return 0;
    }

    private void complete(JobClient client, ActivatedJob job, int assignedCount) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("assigned_count", assignedCount);

        client.newCompleteCommand(job.getKey())
                .variables(vars)
                .send()
                .join();
    }
}
