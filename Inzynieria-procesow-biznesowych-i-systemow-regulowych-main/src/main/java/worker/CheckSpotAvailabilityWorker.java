package worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import service.DatabaseService;

import java.util.HashMap;
import java.util.Map;

@Component
public class CheckSpotAvailabilityWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckSpotAvailabilityWorker.class);

    private final DatabaseService databaseService;

    public CheckSpotAvailabilityWorker(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @JobWorker(type = "check-spot-availbility")
    public void checkSpot(final JobClient client, final ActivatedJob job) {

        LOGGER.info("=== CheckNormalSpotAvailabilityWorker STARTED ===");

        HashMap<String, Object> result = new HashMap<>();

        // Zawsze sprawdzamy kategorię "normal"
        String category = "normal";
        LOGGER.info("Checking free spots for category '{}'", category);

        int freeSpots = -1; // domyślnie -1 = błąd
        try {
            freeSpots = databaseService.getFreeSpots(category); // metoda wykonuje SELECT free_spots WHERE category='normal'
            LOGGER.info("Database returned free spots = {}", freeSpots);
        } catch (Exception e) {
            LOGGER.error("Database query failed", e);
        }

        // Logika dostępności
        boolean spotsAvailable = freeSpots > 0;
        LOGGER.info("Computed availability: {}", spotsAvailable);

        // Ustawiamy zmienne procesu
        result.put("free_spots", freeSpots);      // liczba miejsc (-1 jeśli błąd)
        result.put("spots_available", spotsAvailable); // true jeśli > 0

        // Wysyłamy wynik do Camunda
        client.newCompleteCommand(job.getKey())
                .variables(result)
                .send()
                .join();

        LOGGER.info("=== CheckNormalSpotAvailabilityWorker FINISHED ===");
    }
}
