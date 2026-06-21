package com.cronitor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationChannelRequest {

    @NotBlank(message = "channelType is required")
    private String channelType;   // EMAIL | SLACK | SNS | WEBHOOK

    @NotBlank(message = "configJson is required")
    private String configJson;    // {"to":"ops@acme.com"} for EMAIL, {"webhook_url":"..."} for SLACK
}
