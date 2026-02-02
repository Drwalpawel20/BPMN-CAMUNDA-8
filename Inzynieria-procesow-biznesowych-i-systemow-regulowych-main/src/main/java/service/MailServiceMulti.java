package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MailServiceMulti {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MailServiceMulti.class);

    private final JavaMailSender mailSender;

    public MailServiceMulti(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendBulk(List<String> recipients,
                         String subject,
                         String body) {

        LOGGER.info("Sending mail to {} recipients", recipients.size());

        for (String email : recipients) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject(subject);
                message.setText(body);

                mailSender.send(message);

                LOGGER.info("Mail sent to {}", email);

            } catch (Exception e) {
                LOGGER.error("Failed to send mail to {}", email, e);
            }
        }
    }
}
