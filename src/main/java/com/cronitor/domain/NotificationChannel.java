package com.cronitor.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_channels")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private MonitoredJob job;

    /**
     * EMAIL | SLACK | SNS | WEBHOOK
     * Must match AlertChannel.supports() return values.
     */
    @Column(name = "channel_type", nullable = false)
    private String channelType;

    /**
     * Channel-specific config stored as JSONB.
     * EMAIL: {"to":"ops@acme.com"}
     * SLACK: {"webhook_url":"https://hooks.slack.com/..."}
     * SNS:   {"topic_arn":"arn:aws:sns:..."}
     *
     * @JdbcTypeCode(SqlTypes.JSON) tells Hibernate to treat this
     * as JSON when binding to the PostgreSQL jsonb column,
     * instead of sending it as plain varchar (which Postgres rejects).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", nullable = false, columnDefinition = "jsonb")
    private String configJson;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
