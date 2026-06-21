package com.cronitor.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Delivers alerts to a Slack channel via Slack incoming webhooks.
 * No Slack SDK needed — just a JSON POST to the webhook URL.
 *
 * Expected configJson format:
 * {
 *   "webhook_url": "https://hooks.slack.com/services/T.../B.../..."
 * }
 *
 * To get a webhook URL:
 * Slack → Your workspace → Apps → Incoming Webhooks → Add new webhook
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlackAlertChannel implements AlertChannel {

    private final ObjectMapper objectMapper;

    // Spring 6 / Boot 3 RestClient — modern replacement for RestTemplate
    private final RestClient restClient = RestClient.create();

    @Override
    public String supports() {
        return "SLACK";
    }

    @Override
    public void send(AlertEvent event, String configJson) {
        try {
            JsonNode config = objectMapper.readTree(configJson);
            String webhookUrl = config.get("webhook_url").asText();

            // Slack's incoming webhook expects {"text": "..."}
            // For richer formatting you'd use "blocks" — keeping it simple for now
            Map<String, String> payload = Map.of("text", buildSlackMessage(event));

            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Slack alert sent: type={}, job={}", event.getType(), event.getJobSlug());

        } catch (Exception e) {
            log.error("Failed to send Slack alert for job={}: {}", event.getJobSlug(), e.getMessage(), e);
        }
    }

    private String buildSlackMessage(AlertEvent event) {
        String emoji = switch (event.getType()) {
            case MISSED           -> ":red_circle:";
            case FAILED           -> ":x:";
            case DURATION_ANOMALY -> ":warning:";
        };

        return "%s *[%s]* %s\n%s".formatted(
                emoji,
                event.getType(),
                event.getJobName(),
                event.getMessage()
        );
    }
}
