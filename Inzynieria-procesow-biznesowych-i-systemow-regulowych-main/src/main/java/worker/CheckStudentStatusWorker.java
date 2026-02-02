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
public class CheckStudentStatusWorker {

    private static final Logger LOG = LoggerFactory.getLogger(CheckStudentStatusWorker.class);
    private final DatabaseService databaseService;

    public CheckStudentStatusWorker(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @JobWorker(type = "check-student-status")
    public void handleStudentCheck(final JobClient client, final ActivatedJob job) {
        LOG.info("Check Student Status worker started");

        // Pobranie zmiennych wejściowych
        Map<String, Object> vars = job.getVariablesAsMap();
        String firstName = (String) vars.get("first_name");
        String lastName = (String) vars.get("last_name");
        String indexNumber = (String) vars.get("index");

        // Pobranie statusu studenta z bazy
        Map<String, Object> status = databaseService.getStudentStatus(firstName, lastName, indexNumber);

        // Przygotowanie zmiennych do zwrócenia do subprocessu
        Map<String, Object> outputVars = new HashMap<>();
        // Konwersja na Boolean, aby Camunda poprawnie propagowała zmienne
        outputVars.put("is_student", Boolean.valueOf(String.valueOf(status.get("is_student"))));
        outputVars.put("student_id_valid", Boolean.valueOf(String.valueOf(status.get("student_id_valid"))));

        LOG.info("Student check result: {}", outputVars);

        // CompleteCommand z .variables() propaguje zmienne do nadrzędnego procesu
        client.newCompleteCommand(job.getKey())
                .variables(outputVars)
                .send()
                .join();

        LOG.info("Check Student Status worker completed");
    }
}
