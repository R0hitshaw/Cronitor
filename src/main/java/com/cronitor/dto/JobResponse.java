package com.cronitor.dto;

import com.cronitor.domain.MonitoredJob;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class JobResponse {

    private UUID id;
    private String name;
    private String slug;
    private String cronExpression;
    private int gracePeriodSeconds;
    private String status;
    private Instant lastPingAt;
    private Instant nextExpectedAt;
    private Instant createdAt;

    public static JobResponse from(MonitoredJob job) {
        return JobResponse.builder()
                .id(job.getId())
                .name(job.getName())
                .slug(job.getSlug())
                .cronExpression(job.getCronExpression())
                .gracePeriodSeconds(job.getGracePeriodSeconds())
                .status(job.getStatus())
                .lastPingAt(job.getLastPingAt())
                .nextExpectedAt(job.getNextExpectedAt())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
