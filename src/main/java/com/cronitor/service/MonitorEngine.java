package com.cronitor.service;

import com.cronitor.alert.AlertDispatcher;
import com.cronitor.alert.AlertEvent;
import com.cronitor.domain.MonitoredJob;
import com.cronitor.repository.JobExecutionRepository;
import com.cronitor.repository.MonitoredJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.OptionalDouble;

@Service
@Slf4j
public class MonitorEngine {

    private final MonitoredJobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final AlertDispatcher alertDispatcher;
    private final AlertService alertService;
    private final int alertRetentionDays;

    private static final int ROLLING_WINDOW = 10;
    private static final double ANOMALY_THRESHOLD = 2.0;

    public MonitorEngine(
            MonitoredJobRepository jobRepository,
            JobExecutionRepository executionRepository,
            AlertDispatcher alertDispatcher,
            AlertService alertService,
            @Value("${cronitor.alert.retention-days:90}") int alertRetentionDays) {
        this.jobRepository = jobRepository;
        this.executionRepository = executionRepository;
        this.alertDispatcher = alertDispatcher;
        this.alertService = alertService;
        this.alertRetentionDays = alertRetentionDays;
    }

    @Scheduled(fixedDelayString = "${cronitor.monitor.check-interval-ms:60000}")
    @Transactional
    public void runChecks() {
        log.debug("MonitorEngine cycle started");
        checkMissedRuns();
        checkDurationAnomalies();
        log.debug("MonitorEngine cycle complete");
    }

    /**
     * Nightly cleanup at 2 AM — delete resolved alerts older than retention window.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void purgeOldAlerts() {
        log.info("Running nightly alert history cleanup (retention={}d)", alertRetentionDays);
        alertService.purgeOldAlerts(alertRetentionDays);
    }

    void checkMissedRuns() {
        Instant now = Instant.now();
        List<MonitoredJob> candidates = jobRepository.findOverdueJobs(now);

        for (MonitoredJob job : candidates) {
            Instant deadline = job.getNextExpectedAt()
                    .plusSeconds(job.getGracePeriodSeconds());

            if (now.isAfter(deadline)) {
                log.warn("Missed run detected: job={}, deadline={}", job.getSlug(), deadline);
                job.setStatus("MISSED");
                jobRepository.save(job);
                alertDispatcher.dispatch(
                        AlertEvent.missed(job.getId(), job.getName(), job.getSlug()));
            }
        }
    }

    void checkDurationAnomalies() {
        List<MonitoredJob> runningJobs = jobRepository.findAllByStatus("RUNNING");

        for (MonitoredJob job : runningJobs) {
            if (job.getLastPingAt() == null) continue;

            long elapsedMs = Instant.now().toEpochMilli() - job.getLastPingAt().toEpochMilli();
            OptionalDouble avgMs = computeRollingAverage(job);
            if (avgMs.isEmpty()) continue;

            if (elapsedMs > avgMs.getAsDouble() * ANOMALY_THRESHOLD) {
                log.warn("Duration anomaly: job={}, elapsedMs={}, avgMs={}",
                        job.getSlug(), elapsedMs, (long) avgMs.getAsDouble());
                alertDispatcher.dispatch(AlertEvent.durationAnomaly(
                        job.getId(), job.getName(), job.getSlug(),
                        elapsedMs, (long) avgMs.getAsDouble()));
            }
        }
    }

    private OptionalDouble computeRollingAverage(MonitoredJob job) {
        var recentRuns = executionRepository.findRecentSuccessfulExecutions(
                job.getId(), PageRequest.of(0, ROLLING_WINDOW));
        if (recentRuns.size() < 3) return OptionalDouble.empty();
        return recentRuns.stream()
                .mapToLong(e -> e.getDurationMs() != null ? e.getDurationMs() : 0L)
                .average();
    }
}
