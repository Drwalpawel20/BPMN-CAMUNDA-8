package worker;

import exception.EmailSendException;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import service.MailService;

import java.util.HashMap;
import java.util.Map;

@Component
public class SendDecisionEmailWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendDecisionEmailWorker.class);

    private final MailService mailService;

    public SendDecisionEmailWorker(MailService mailService) {
        this.mailService = mailService;
    }

    @JobWorker(type = "send-decision-email")
    public void sendEmail(final JobClient client, final ActivatedJob job) {
        LOGGER.info("=== SendDecisionEmailWorker STARTED ===");

        // Pobranie zmiennych procesu
        String employeeEmail = job.getVariable("email").toString();
        String decision = job.getVariable("decision").toString();

        try {
            if (employeeEmail == null || decision == null) {
                throw new EmailSendException("Brak zmiennych Email lub decision.");
            }

            String subject = "Decyzja w sprawie pozwolenia na parkowanie";
            String body = "Dzień dobry,\n\nInformujemy o statusie Twojego wniosku o przydział miejsca parkingowego:\n\nDecyzja: " + decision + "\n\nPozdrawiamy serdecznie,\n\nZespół Parkingowy";

            boolean sent = mailService.sendEmail(employeeEmail, subject, body);

            if (!sent) {
                throw new EmailSendException("Nie udało się wysłać maila na adres: " + employeeEmail);
            }

            // Jeśli tutaj doszliśmy → email wysłany → normalne zakończenie
            Map<String, Object> result = new HashMap<>();
            result.put("emailSent", true);

            client.newCompleteCommand(job.getKey())
                    .variables(result)
                    .send()
                    .join();

            LOGGER.info("=== SendDecisionEmailWorker FINISHED OK ===");

        } catch (EmailSendException ex) {

            LOGGER.error("SendDecisionEmailWorker ERROR: " + ex.getMessage());

            // ZGŁOSZENIE BPMN ERROR → przechwytuje Boundary Event
            client.newThrowErrorCommand(job.getKey())
                    .errorCode("EMAIL_FAILED")     // <-- TO MUSI BYĆ W BPMN
                    .errorMessage(ex.getMessage())
                    .send()
                    .join();
        }
    }
}
