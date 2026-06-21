package com.cronitor.controller;

import com.cronitor.dto.ExecutionResponse;
import com.cronitor.dto.StatsResponse;
import com.cronitor.repository.JobExecutionRepository;
import com.cronitor.service.StatsService;
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
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "Job statistics and execution history")
public class StatsController {

    private final StatsService statsService;
    private final JobExecutionRepository executionRepository;

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get stats for a job — success rate, p95 duration, counts")
    public ResponseEntity<StatsResponse> getStats(@PathVariable UUID id) {
        return ResponseEntity.ok(statsService.getStats(id));
    }

    /**
     * Paginated execution history for a job.
     * Default: page 0, 20 results per page, newest first.
     *
     * Usage: GET /api/jobs/{id}/executions?page=0&size=20
     */
    @GetMapping("/{id}/executions")
    @Operation(summary = "Paginated execution history for a job")
    public ResponseEntity<Page<ExecutionResponse>> getExecutions(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ExecutionResponse> page = executionRepository
                .findAllByJobIdOrderByStartedAtDesc(id, pageable)
                .map(ExecutionResponse::from);

        return ResponseEntity.ok(page);
    }
}
