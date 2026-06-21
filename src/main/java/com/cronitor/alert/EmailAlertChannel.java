package com.cronitor.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Delivers alerts via email using Spring's JavaMailSender.
 *
 * Expected configJson format:
 * {
 *   "to": "ops@acme.com",
 *   "cc": "manager@acme.com"   (optional)
 * }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailAlertChannel implements AlertChannel {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @Override
    public String supports() {
        return "EMAIL";
    }

    @Override
    public void send(AlertEvent event, String configJson) {
        try {
            JsonNode config = objectMapper.readTree(configJson);
            String to = config.get("to").asText();

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(to);

            // Optional CC
            if (config.has("cc")) {
                mail.setCc(config.get("cc").asText());
            }

            mail.setSubject(buildSubject(event));
            mail.setText(buildBody(event));

            mailSender.send(mail);
            log.info("Email alert sent: type={}, job={}, to={}", event.getType(), event.getJobSlug(), to);

        } catch (Exception e) {
            // Never rethrow — a failed email must not block other channels from firing
            log.error("Failed to send email alert for job={}: {}", event.getJobSlug(), e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Email content builders
    // ---------------------------------------------------------------

    private String buildSubject(AlertEvent event) {
        return switch (event.getType()) {
            case MISSED           -> "[CRONITOR] MISSED: " + event.getJobName();
            case FAILED           -> "[CRONITOR] FAILED: " + event.getJobName();
            case DURATION_ANOMALY -> "[CRONITOR] SLOW: "   + event.getJobName();
        };
    }

    private String buildBody(AlertEvent event) {
        return """
                Cronitor Alert
                ──────────────────────────────
                Job:     %s
                Slug:    %s
                Type:    %s
                Time:    %s

                %s
                ──────────────────────────────
                View job: http://localhost:8080/api/jobs (replace with your deployed URL)
                """.formatted(
                event.getJobName(),
                event.getJobSlug(),
                event.getType(),
                event.getOccurredAt(),
                event.getMessage()
        );
    }
}
