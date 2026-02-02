package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Service
public class CandidateEmailService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CandidateEmailService.class);

    private final DatabaseService databaseService;
    private final MailServiceMulti mailServiceMulti;

    public CandidateEmailService(DatabaseService databaseService,
                                 MailServiceMulti mailServiceMulti) {
        this.databaseService = databaseService;
        this.mailServiceMulti = mailServiceMulti;
    }

    public void sendDecisionEmails() {

        List<String> acceptedEmails = new ArrayList<>();
        List<String> rejectedEmails = new ArrayList<>();

        String sql =
                "SELECT email, qualified " +
                        "FROM parking.candidates " +
                        "WHERE email IS NOT NULL";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int total = 0;

            while (rs.next()) {
                total++;
                boolean qualified = rs.getBoolean("qualified");
                String email = rs.getString("email");

                if (qualified) {
                    acceptedEmails.add(email);
                } else {
                    rejectedEmails.add(email);
                }
            }

            LOGGER.info("Candidates loaded: {}", total);
            LOGGER.info("Qualified: {}", acceptedEmails.size());
            LOGGER.info("Rejected: {}", rejectedEmails.size());

        } catch (Exception e) {
            LOGGER.error("Failed to fetch candidates for emails", e);
            throw new RuntimeException(e);
        }

        if (!acceptedEmails.isEmpty()) {
            mailServiceMulti.sendBulk(
                    acceptedEmails,
                    "Decyzja: przyznanie miejsca parkingowego",
                    "Gratulujemy! Zakwalifikowałeś/aś się do otrzymania miejsca parkingowego."
            );
        }

        if (!rejectedEmails.isEmpty()) {
            mailServiceMulti.sendBulk(
                    rejectedEmails,
                    "Decyzja: brak miejsca parkingowego",
                    "Dziękujemy za udział. Niestety nie udało się przyznać miejsca parkingowego."
            );
        }
    }
}
