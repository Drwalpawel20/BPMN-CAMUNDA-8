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
import java.util.Map;

@Component
public class SaveCandidatesWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveCandidatesWorker.class);

    private final DatabaseService databaseService;

    public SaveCandidatesWorker(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @JobWorker(type = "save_to_database")
    public void handle(JobClient client, ActivatedJob job) {

        LOGGER.info("=== SaveCandidatesWorker STARTED ===");

        // pobieramy wszystkie zmienne formularza
        Map<String, Object> vars = job.getVariablesAsMap();

        String firstName = (String) vars.get("first_name");
        String lastName = (String) vars.get("last_name");
        String indexNumber = (String) vars.get("index");
        String city = (String) vars.get("miasto");
        String email = (String) vars.get("email");

        String sql = """
    INSERT INTO parking.candidates
    (first_name, last_name, index_number, city, score, qualified, email)
    VALUES (?, ?, ?, ?, ?, ?, ?)
    """;


        try (Connection conn = databaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, indexNumber);
            ps.setString(4, city);
            ps.setInt(5, 0);          // score
            ps.setBoolean(6, false);  // qualified
            ps.setString(7, email);



            ps.executeUpdate();

        } catch (Exception ex) {
            LOGGER.error("Błąd podczas zapisu kandydata", ex);
            throw new RuntimeException("Nie udało się zapisać kandydata", ex);
        }

        LOGGER.info("=== SaveCandidatesWorker FINISHED: zapis zakończony poprawnie ===");

        client.newCompleteCommand(job.getKey())
                .variables("""
                        {"candidate_saved": true}
                        """)
                .send();
    }
}
