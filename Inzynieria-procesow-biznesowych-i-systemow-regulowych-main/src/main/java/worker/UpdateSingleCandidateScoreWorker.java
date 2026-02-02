package worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import service.DatabaseService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Component
public class UpdateSingleCandidateScoreWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateSingleCandidateScoreWorker.class);

    private final DatabaseService databaseService;
    private static final String API_KEY = "YOUR GEO API KEY HERE";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public UpdateSingleCandidateScoreWorker(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @JobWorker(type = "update_single_candidate_score")
    public void handle(JobClient jobClient, ActivatedJob job) {

        LOGGER.info("=== UpdateSingleCandidateScoreWorker STARTED ===");

        String indexNumber = (String) job.getVariable("index");
        if (indexNumber == null) {
            throw new RuntimeException("Brak zmiennej 'index'");
        }

        try (Connection conn = databaseService.getConnection()) {

            // pobieramy dane kandydata z bazy
            String city;
            int candidateId;
            String sqlSelect = "SELECT id, city FROM parking.candidates WHERE index_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
                ps.setString(1, indexNumber);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new RuntimeException("Nie znaleziono kandydata o index_number = " + indexNumber);
                    }
                    candidateId = rs.getInt("id");
                    city = rs.getString("city");
                }
            }

            // liczymy odległość od Tarnowa
            double distance = getDistanceFromTarnow(city);

            // aktualizujemy wynik w bazie
            String sqlUpdate = "UPDATE parking.candidates SET score = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setDouble(1, distance);
                ps.setInt(2, candidateId);
                ps.executeUpdate();
            }

            LOGGER.info("Updated score for candidate id={} to {} km", candidateId, distance);

            // kończymy job
            jobClient.newCompleteCommand(job.getKey())
                    .variables("{\"score_updated\": true}")
                    .send()
                    .join();

        } catch (Exception e) {
            LOGGER.error("Failed to update score", e);

            jobClient.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage(e.getMessage())
                    .send()
                    .join();
        }
    }

    private double getDistanceFromTarnow(String city) throws Exception {
        double[] start = geocode(city);
        double[] end = geocode("Tarnów");

        String url = "https://api.openrouteservice.org/v2/directions/driving-car"
                + "?api_key=" + API_KEY
                + "&start=" + start[0] + "," + start[1]
                + "&end=" + end[0] + "," + end[1];

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Route API error: " + response.statusCode());
        }

        JsonNode root = mapper.readTree(response.body());
        double distanceMeters = root.get("features").get(0)
                .get("properties").get("segments").get(0)
                .get("distance").asDouble();

        return distanceMeters / 1000.0; // km
    }

    private double[] geocode(String query) throws Exception {
        String url = "https://api.openrouteservice.org/geocode/search"
                + "?api_key=" + API_KEY
                + "&text=" + query.replace(" ", "%20");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Geocoding API error: " + response.statusCode());
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode coords = root.get("features").get(0).get("geometry").get("coordinates");

        double lon = coords.get(0).asDouble();
        double lat = coords.get(1).asDouble();

        return new double[]{lon, lat};
    }
}
