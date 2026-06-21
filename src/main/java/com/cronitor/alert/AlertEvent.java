package com.cronitor.alert;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable data carrier representing a single alert event.
 *
 * Built by MonitorEngine when a problem is detected.
 * Consumed by AlertDispatcher to route to the correct channel.
 *
 * Deliberately has no JPA or Spring dependencies — pure data.
 */
@Getter
@Builder
public class AlertEvent {

    public enum Type {
        MISSED,            // job did not ping /finish before next_expected_at + grace
        FAILED,            // job explicitly pinged /fail
        DURATION_ANOMALY   // job is RUNNING but taking > 2x its rolling average
    }

    private final UUID jobId;
    private final String jobName;
    private final String jobSlug;
    private final Type type;
    private final String message;
    private final UUID executionId;   // nullable — MISSED alerts have no execution
    private final Instant occurredAt;

    // ---------------------------------------------------------------
    // Static factories — one per alert type for readable call sites
    // ---------------------------------------------------------------

    public static AlertEvent missed(UUID jobId, String jobName, String jobSlug) {
        return AlertEvent.builder()
                .jobId(jobId)
                .jobName(jobName)
                .jobSlug(jobSlug)
                .type(Type.MISSED)
                .message(String.format(
                        "Job '%s' missed its scheduled run. It has not pinged /finish on time.",
                        jobName))
                .occurredAt(Instant.now())
                .build();
    }

    public static AlertEvent failed(UUID jobId, String jobName, String jobSlug,
                                    UUID executionId, String exitMessage) {
        return AlertEvent.builder()
                .jobId(jobId)
                .jobName(jobName)
                .jobSlug(jobSlug)
                .type(Type.FAILED)
                .executionId(executionId)
                .message(String.format(
                        "Job '%s' reported a failure. Exit message: %s",
                        jobName, exitMessage != null ? exitMessage : "none"))
                .occurredAt(Instant.now())
                .build();
    }

    public static AlertEvent durationAnomaly(UUID jobId, String jobName, String jobSlug,
                                              long elapsedMs, long avgMs) {
        return AlertEvent.builder()
                .jobId(jobId)
                .jobName(jobName)
                .jobSlug(jobSlug)
                .type(Type.DURATION_ANOMALY)
                .message(String.format(
                        "Job '%s' is taking unusually long. Elapsed: %ds, average: %ds.",
                        jobName, elapsedMs / 1000, avgMs / 1000))
                .occurredAt(Instant.now())
                .build();
    }
}
