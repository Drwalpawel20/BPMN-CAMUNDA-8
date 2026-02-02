package service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Wysyła maila na wskazany adres z podanym tematem i treścią.
     *
     * @param to      adres odbiorcy
     * @param subject temat maila
     * @param body    treść maila
     * @return true jeśli mail został wysłany poprawnie, false w przeciwnym wypadku
     */
    public boolean sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            LOGGER.warn("MailService: adres odbiorcy jest pusty");
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            LOGGER.info("MailService: wysłano mail do {}", to);
            return true;

        } catch (Exception e) {
            LOGGER.error("MailService: nie udało się wysłać maila do {}", to, e);
            return false;
        }
    }
}
