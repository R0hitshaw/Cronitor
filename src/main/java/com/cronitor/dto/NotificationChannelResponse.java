package com.cronitor.dto;

import com.cronitor.domain.NotificationChannel;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationChannelResponse {

    private UUID id;
    private UUID jobId;
    private String channelType;
    private String configJson;
    private boolean isActive;
    private Instant createdAt;

    public static NotificationChannelResponse from(NotificationChannel channel) {
        return NotificationChannelResponse.builder()
                .id(channel.getId())
                .jobId(channel.getJob().getId())
                .channelType(channel.getChannelType())
                .configJson(channel.getConfigJson())
                .isActive(channel.isActive())
                .createdAt(channel.getCreatedAt())
                .build();
    }
}
