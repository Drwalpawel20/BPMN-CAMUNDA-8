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
public class CheckEmployeeWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckEmployeeWorker.class);

    private final DatabaseService databaseService;

    public CheckEmployeeWorker(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @JobWorker(type = "employee-id-validator-worker")
    public void checkEmployee(final JobClient client, final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        Map<String, Object> result = new HashMap<>();

        try {
            String firstName = (String) vars.get("first_name");
            String lastName = (String) vars.get("last_name");
            String staff_id = (String) vars.get("index");

            LOGGER.info("Checking employee: {} {}", firstName, lastName);

            // Tutaj proste sprawdzenie w bazie lub serwisie bez entity
            boolean isValid = databaseService.getEmployeeIdValidity(firstName, lastName, staff_id);

            LOGGER.info("Employee {} {} validity: {}", firstName, lastName, isValid);

            result.put("employee_id_valid", isValid);

            // Zakończenie joba i przekazanie zmiennych do procesu
            client.newCompleteCommand(job.getKey())
                    .variables(result)
                    .send()
                    .join();

        } catch (Exception e) {
            LOGGER.error("Error checking employee", e);

            // Zgłoszenie błędu do Camundy, jeśli coś pójdzie nie tak
            client.newThrowErrorCommand(job.getKey())
                    .errorCode("EMPLOYEE_CHECK_FAILED")
                    .errorMessage(e.getMessage())
                    .send()
                    .join();
        }
    }
}