package com.cronitor.controller;

import com.cronitor.dto.ExecutionResponse;
import com.cronitor.dto.PingRequest;
import com.cronitor.service.HeartbeatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ping")
@RequiredArgsConstructor
@Tag(name = "Ping", description = "Heartbeat endpoints called by your cron jobs")
public class PingController {

    private final HeartbeatService heartbeatService;

    /**
     * Call this when your cron job starts.
     *
     * Example (curl):
     *   curl -X POST https://yourhost/api/ping/nightly-billing-job/start \
     *        -H "Content-Type: application/json" \
     *        -d '{"runId": "run-20240601-001", "message": "Billing job started"}'
     */
    @PostMapping("/{slug}/start")
    @Operation(summary = "Record job start — call at the beginning of your cron job")
    public ResponseEntity<ExecutionResponse> start(
            @PathVariable String slug,
            @RequestBody(required = false) PingRequest request) {
        return ResponseEntity.ok(
                heartbeatService.recordStart(slug, request != null ? request : new PingRequest()));
    }

    /**
     * Call this when your cron job finishes successfully.
     * This resets the "next expected at" clock based on the cron expression.
     */
    @PostMapping("/{slug}/finish")
    @Operation(summary = "Record job success — resets the missed-run clock")
    public ResponseEntity<ExecutionResponse> finish(
            @PathVariable String slug,
            @RequestBody(required = false) PingRequest request) {
        return ResponseEntity.ok(
                heartbeatService.recordFinish(slug, request != null ? request : new PingRequest()));
    }

    /**
     * Call this if your cron job encounters a fatal error.
     * This marks the job FAILED and triggers alert rules.
     */
    @PostMapping("/{slug}/fail")
    @Operation(summary = "Record job failure — triggers FAILED alerts")
    public ResponseEntity<ExecutionResponse> fail(
            @PathVariable String slug,
            @RequestBody(required = false) PingRequest request) {
        return ResponseEntity.ok(
                heartbeatService.recordFail(slug, request != null ? request : new PingRequest()));
    }
}
