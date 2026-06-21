package com.cronitor.dto;

import com.cronitor.domain.AlertHistory;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AlertHistoryResponse {

    private UUID id;
    private UUID jobId;
    private UUID executionId;
    private String severity;
    private String message;
    private boolean resolved;
    private Instant firedAt;
    private Instant resolvedAt;

    public static AlertHistoryResponse from(AlertHistory alert) {
        return AlertHistoryResponse.builder()
                .id(alert.getId())
                .jobId(alert.getJobId())
                .executionId(alert.getExecutionId())
                .severity(alert.getSeverity())
                .message(alert.getMessage())
                .resolved(alert.getResolved())
                .firedAt(alert.getFiredAt())
                .resolvedAt(alert.getResolvedAt())
                .build();
    }
}
