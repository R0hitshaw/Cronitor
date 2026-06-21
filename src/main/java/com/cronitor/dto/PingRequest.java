package com.cronitor.dto;

import lombok.Data;

@Data
public class PingRequest {

    /**
     * Optional caller-supplied idempotency key.
     * If provided, a duplicate START ping with the same run_id is safely ignored.
     */
    private String runId;

    /**
     * Optional free-text message (exit reason, error summary, etc.)
     */
    private String message;
}
