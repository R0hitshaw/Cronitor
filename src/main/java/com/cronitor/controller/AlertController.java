package com.cronitor.controller;

import com.cronitor.dto.AlertHistoryResponse;
import com.cronitor.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert history and resolution")
public class AlertController {

    private final AlertService alertService;

    /**
     * All alerts across all jobs, newest first.
     * Usage: GET /api/alerts/history?page=0&size=20
     */
    @GetMapping("/history")
    @Operation(summary = "Paginated alert history across all jobs")
    public ResponseEntity<Page<AlertHistoryResponse>> getHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(alertService.listAll(pageable));
    }

    /**
     * Manually resolve a firing alert.
     * Called from the dashboard when an operator acknowledges an issue.
     */
    @PostMapping("/{id}/resolve")
    @Operation(summary = "Manually resolve a firing alert")
    public ResponseEntity<AlertHistoryResponse> resolve(@PathVariable UUID id) {
        return ResponseEntity.ok(alertService.resolve(id));
    }
}
