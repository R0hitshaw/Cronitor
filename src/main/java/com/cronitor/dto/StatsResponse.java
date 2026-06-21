package com.cronitor.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Complete stats snapshot for a single job.
 * Returned by GET /api/jobs/{id}/stats
 *
 * Designed to give the dashboard everything it needs in one call:
 * - Reliability metrics (success rate, counts)
 * - Duration metrics (avg, p95 — for trend charts)
 * - Current state (last run, next expected)
 */
@Data
@Builder
public class StatsResponse {

    // ── Identity ────────────────────────────────────────────────
    private String jobName;
    private String jobSlug;
    private String currentStatus;

    // ── Reliability (last 30 days) ───────────────────────────────
    /** Total number of runs in the last 30 days */
    private long totalRuns;

    /** Runs that completed with SUCCESS status */
    private long successfulRuns;

    /** Runs that pinged /fail */
    private long failedRuns;

    /** Runs the monitor engine marked as MISSED */
    private long missedRuns;

    /**
     * Success rate as a percentage (0.0 – 100.0)
     * Calculated as: successfulRuns / totalRuns * 100
     * Returns 0.0 if no runs exist yet.
     */
    private double successRatePercent;

    // ── Duration metrics (last 50 successful runs) ────────────────
    /**
     * Average duration in milliseconds across recent successful runs.
     * Null if no successful runs exist yet.
     */
    private Long avgDurationMs;

    /**
     * 95th percentile duration in milliseconds.
     * "95% of runs finish within this time."
     * Better than average for catching outliers.
     * Null if fewer than 5 successful runs exist.
     */
    private Long p95DurationMs;

    /**
     * Fastest recorded run duration in ms.
     */
    private Long minDurationMs;

    /**
     * Slowest recorded run duration in ms.
     */
    private Long maxDurationMs;

    // ── Alert summary ─────────────────────────────────────────────
    /** Number of unresolved alerts currently firing for this job */
    private long activeAlertCount;
}
