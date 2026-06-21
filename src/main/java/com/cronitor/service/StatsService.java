package com.cronitor.service;

import com.cronitor.dto.StatsResponse;
import com.cronitor.exception.JobNotFoundException;
import com.cronitor.repository.AlertHistoryRepository;
import com.cronitor.repository.JobExecutionRepository;
import com.cronitor.repository.MonitoredJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final MonitoredJobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final AlertHistoryRepository alertHistoryRepository;

    // Look-back window for reliability metrics
    private static final int RELIABILITY_WINDOW_DAYS = 30;

    // Number of recent runs used for duration metrics
    private static final int DURATION_WINDOW = 50;

    // Minimum runs needed to compute meaningful p95
    private static final int MIN_RUNS_FOR_P95 = 5;

    @Transactional(readOnly = true)
    public StatsResponse getStats(UUID jobId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        Instant since = Instant.now().minus(RELIABILITY_WINDOW_DAYS, ChronoUnit.DAYS);

        // ── Reliability counts ────────────────────────────────────
        long totalRuns    = executionRepository.countByJobIdAndStatusSince(jobId, null, since);
        long successRuns  = executionRepository.countByJobIdAndStatusSince(jobId, "SUCCESS", since);
        long failedRuns   = executionRepository.countByJobIdAndStatusSince(jobId, "FAILED", since);
        long missedRuns   = alertHistoryRepository.countMissedAlertsSince(jobId, since);

        double successRate = totalRuns == 0 ? 0.0
                : Math.round((successRuns * 100.0 / totalRuns) * 10.0) / 10.0;

        // ── Duration metrics ──────────────────────────────────────
        List<Long> durations = executionRepository
                .findRecentSuccessfulExecutions(jobId, PageRequest.of(0, DURATION_WINDOW))
                .stream()
                .map(e -> e.getDurationMs() != null ? e.getDurationMs() : 0L)
                .sorted()
                .toList();

        Long avgDuration = durations.isEmpty() ? null
                : (long) durations.stream().mapToLong(Long::longValue).average().orElse(0);

        Long p95Duration = durations.size() < MIN_RUNS_FOR_P95 ? null
                : computePercentile(durations, 95);

        Long minDuration = durations.isEmpty() ? null : Collections.min(durations);
        Long maxDuration = durations.isEmpty() ? null : Collections.max(durations);

        // ── Active alerts ─────────────────────────────────────────
        long activeAlerts = alertHistoryRepository.countByJobIdAndResolvedFalse(jobId);

        return StatsResponse.builder()
                .jobName(job.getName())
                .jobSlug(job.getSlug())
                .currentStatus(job.getStatus())
                .totalRuns(totalRuns)
                .successfulRuns(successRuns)
                .failedRuns(failedRuns)
                .missedRuns(missedRuns)
                .successRatePercent(successRate)
                .avgDurationMs(avgDuration)
                .p95DurationMs(p95Duration)
                .minDurationMs(minDuration)
                .maxDurationMs(maxDuration)
                .activeAlertCount(activeAlerts)
                .build();
    }

    // ---------------------------------------------------------------
    // p95 calculation
    // ---------------------------------------------------------------

    /**
     * Computes the Nth percentile from a SORTED list of values.
     *
     * Formula: index = ceil(percentile / 100 * size) - 1
     *
     * Example with 20 values and p95:
     *   index = ceil(0.95 * 20) - 1 = ceil(19) - 1 = 18
     *   → returns the value at position 18 (0-indexed) in the sorted list
     *
     * The list MUST be sorted ascending before calling this.
     */
    private long computePercentile(List<Long> sortedValues, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, index));
    }
}
