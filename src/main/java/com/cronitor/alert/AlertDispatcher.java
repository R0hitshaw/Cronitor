package com.cronitor.alert;

import com.cronitor.domain.AlertHistory;
import com.cronitor.domain.MonitoredJob;
import com.cronitor.repository.AlertHistoryRepository;
import com.cronitor.repository.MonitoredJobRepository;
import com.cronitor.repository.NotificationChannelRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central alert router.
 *
 * Responsibilities:
 * 1. Deduplication via Redis — don't re-fire the same alert within TTL window
 * 2. Fetch the job's configured notification channels from DB
 * 3. Route to the correct AlertChannel implementation (strategy pattern)
 * 4. Persist every fired alert to alert_history
 */
@Service
@Slf4j
public class AlertDispatcher {

    private final Map<String, AlertChannel> channelMap;
    private final NotificationChannelRepository channelRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final MonitoredJobRepository jobRepository;
    private final StringRedisTemplate redisTemplate;
    private final long dedupTtlSeconds;

    /**
     * Spring injects ALL AlertChannel beans as a List.
     * We index them by their supported channel type for O(1) lookup.
     */
    public AlertDispatcher(
            List<AlertChannel> channels,
            NotificationChannelRepository channelRepository,
            AlertHistoryRepository alertHistoryRepository,
            MonitoredJobRepository jobRepository,
            StringRedisTemplate redisTemplate,
            @Value("${cronitor.alert.dedup-ttl-seconds:3600}") long dedupTtlSeconds) {

        this.channelMap = channels.stream()
                .collect(Collectors.toMap(AlertChannel::supports, c -> c));
        this.channelRepository = channelRepository;
        this.alertHistoryRepository = alertHistoryRepository;
        this.jobRepository = jobRepository;
        this.redisTemplate = redisTemplate;
        this.dedupTtlSeconds = dedupTtlSeconds;

        log.info("AlertDispatcher initialized with channels: {}", channelMap.keySet());
    }

    /**
     * Main entry point — called by MonitorEngine when a problem is detected.
     */
    public void dispatch(AlertEvent event) {
        // Step 1: Deduplication check
        String dedupKey = buildDedupKey(event);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
            log.debug("Alert suppressed (dedup): type={}, job={}", event.getType(), event.getJobSlug());
            return;
        }

        // Step 2: Fetch active notification channels for this job
        var notificationChannels = channelRepository
                .findAllByJobIdAndIsActiveTrue(event.getJobId());

        if (notificationChannels.isEmpty()) {
            log.warn("No active notification channels for job={} — alert not sent", event.getJobSlug());
            // Still persist to history and set dedup key so we don't spam the log
        }

        // Step 3: Route to each configured channel
        for (var channel : notificationChannels) {
            AlertChannel implementation = channelMap.get(channel.getChannelType());
            if (implementation == null) {
                log.warn("No implementation found for channel type={}", channel.getChannelType());
                continue;
            }
            implementation.send(event, channel.getConfigJson());
        }

        // Step 4: Persist to alert_history
        persistAlertHistory(event);

        // Step 5: Set Redis dedup key — suppress re-firing for TTL window
        redisTemplate.opsForValue().set(dedupKey, "1", Duration.ofSeconds(dedupTtlSeconds));
        log.info("Alert dispatched: type={}, job={}, channels={}",
                event.getType(), event.getJobSlug(), notificationChannels.size());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Dedup key format: alert:{TYPE}:{jobId}
     * Example: alert:MISSED:550e8400-e29b-41d4-a716-446655440000
     *
     * This means the same alert type won't re-fire for the same job
     * within the TTL window, even if the scheduler runs 60 more times.
     */
    private String buildDedupKey(AlertEvent event) {
        return "alert:" + event.getType().name() + ":" + event.getJobId();
    }

    private void persistAlertHistory(AlertEvent event) {
        try {
            MonitoredJob job = jobRepository.getReferenceById(event.getJobId());
            AlertHistory history = AlertHistory.builder()
                    .job(job)
                    .executionId(event.getExecutionId())
                    .severity(resolveSeverity(event.getType()))
                    .message(event.getMessage())
                    .resolved(false)
                    .build();
            alertHistoryRepository.save(history);
        } catch (Exception e) {
            // Persistence failure must never block the alert from going out
            log.error("Failed to persist alert history for job={}: {}", event.getJobSlug(), e.getMessage());
        }
    }

    private String resolveSeverity(AlertEvent.Type type) {
        return switch (type) {
            case MISSED           -> "CRITICAL";
            case FAILED           -> "CRITICAL";
            case DURATION_ANOMALY -> "WARNING";
        };
    }
}
