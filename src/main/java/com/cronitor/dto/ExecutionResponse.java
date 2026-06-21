package com.cronitor.dto;

import com.cronitor.domain.JobExecution;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ExecutionResponse {

    private UUID id;
    private UUID jobId;
    private String runId;
    private String status;
    private Instant startedAt;
    private Instant finishedAt;
    private Long durationMs;
    private String exitMessage;

    public static ExecutionResponse from(JobExecution e) {
        return ExecutionResponse.builder()
                .id(e.getId())
                .jobId(e.getJob().getId())
                .runId(e.getRunId())
                .status(e.getStatus())
                .startedAt(e.getStartedAt())
                .finishedAt(e.getFinishedAt())
                .durationMs(e.getDurationMs())
                .exitMessage(e.getExitMessage())
                .build();
    }
}
