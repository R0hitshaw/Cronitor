package com.cronitor.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monitored_jobs")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoredJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    /**
     * URL-safe identifier used in ping endpoints.
     * Example: "nightly-billing-job" → POST /api/ping/nightly-billing-job/finish
     */
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    /**
     * Extra seconds allowed past next_expected_at before marking MISSED.
     * Default: 5 minutes (300s)
     */
    @Column(name = "grace_period_seconds", nullable = false)
    @Builder.Default
    private int gracePeriodSeconds = 300;

    /**
     * PENDING | HEALTHY | RUNNING | MISSED | FAILED
     */
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "last_ping_at")
    private Instant lastPingAt;

    /**
     * Computed on each successful finish ping.
     * The monitor engine queries: WHERE next_expected_at + grace_period < NOW()
     */
    @Column(name = "next_expected_at")
    private Instant nextExpectedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
