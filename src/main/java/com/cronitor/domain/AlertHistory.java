package com.cronitor.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_history")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private MonitoredJob job;

    @Column(name = "job_id", insertable = false, updatable = false)
    private UUID jobId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "execution_id")
    private UUID executionId;

    /**
     * INFO | WARNING | CRITICAL
     */
    @Column(nullable = false)
    @Builder.Default
    private String severity = "WARNING";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "fired_at", nullable = false)
    @Builder.Default
    private Instant firedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
