package com.cronitor.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_executions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private MonitoredJob job;

    /**
     * Optional caller-supplied idempotency key (e.g. a CI run ID).
     * If supplied, a second START ping with the same run_id is a no-op.
     */
    @Column(name = "run_id")
    private String runId;

    /**
     * RUNNING | SUCCESS | FAILED
     */
    @Column(nullable = false)
    @Builder.Default
    private String status = "RUNNING";

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "exit_message", columnDefinition = "TEXT")
    private String exitMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
