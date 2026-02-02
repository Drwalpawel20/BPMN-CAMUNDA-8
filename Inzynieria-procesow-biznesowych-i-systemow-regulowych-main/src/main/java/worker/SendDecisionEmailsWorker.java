package worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import service.CandidateEmailService;

@Component
public class SendDecisionEmailsWorker {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SendDecisionEmailsWorker.class);

    private final CandidateEmailService candidateEmailService;

    public SendDecisionEmailsWorker(CandidateEmailService candidateEmailService) {
        this.candidateEmailService = candidateEmailService;
    }

    @JobWorker(type = "send-decisions-emails")
    public void handle(JobClient client, ActivatedJob job) {

        LOGGER.info("=== SendDecisionEmailsWorker STARTED ===");

        candidateEmailService.sendDecisionEmails();

        client.newCompleteCommand(job.getKey())
                .send()
                .join();

        LOGGER.info("=== SendDecisionEmailsWorker FINISHED ===");
    }
}
