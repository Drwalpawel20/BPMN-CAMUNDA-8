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
public class RankStudentsWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RankStudentsWorker.class);

    private final DatabaseService databaseService;

    public RankStudentsWorker(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @JobWorker(type = "rank_students_by_distance")
    public void handle(JobClient client, ActivatedJob job) {

        LOGGER.info("=== RankStudentsWorker STARTED ===");

        int freeSpots = getFreeSpots();
        LOGGER.info("Free spots available: {}", freeSpots);

        int qualifiedCount = 0;

        try (Connection conn = databaseService.getConnection()) {

            conn.setAutoCommit(false);

            // 1. Reset poprzednich kwalifikacji
            String resetSql =
                    "UPDATE parking.candidates SET qualified = FALSE";
            try (PreparedStatement ps = conn.prepareStatement(resetSql)) {
                ps.executeUpdate();
            }

            // 2. Zakwalifikuj TOP-N kandydatów
            String qualifySql =
                    "UPDATE parking.candidates SET qualified = TRUE " +
                            "WHERE id IN ( " +
                            "   SELECT id FROM parking.candidates " +
                            "   ORDER BY score DESC " +
                            "   LIMIT ? " +
                            ")";

            try (PreparedStatement ps = conn.prepareStatement(qualifySql)) {
                ps.setInt(1, freeSpots);
                qualifiedCount = ps.executeUpdate();
            }

            conn.commit();

            LOGGER.info("Qualified {} candidates", qualifiedCount);

        } catch (Exception e) {
            LOGGER.error("Ranking failed", e);
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("qualified_count", qualifiedCount);

        client.newCompleteCommand(job.getKey())
                .variables(vars)
                .send()
                .join();

        LOGGER.info("=== RankStudentsWorker FINISHED ===");
    }

    private int getFreeSpots() {
        String sql =
                "SELECT SUM(free_spots) AS total_free " +
                        "FROM parking.parking_spots " +
                        "WHERE category = 'normal'";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("total_free");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching free spots", e);
        }
        return 0;
    }
}
