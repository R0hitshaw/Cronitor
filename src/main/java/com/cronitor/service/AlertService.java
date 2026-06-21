package com.cronitor.service;

import com.cronitor.domain.AlertHistory;
import com.cronitor.dto.AlertHistoryResponse;
import com.cronitor.exception.JobNotFoundException;
import com.cronitor.repository.AlertHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertHistoryRepository alertHistoryRepository;

    /**
     * Paginated list of all alerts across all jobs, newest first.
     * The dashboard alert history table calls this.
     */
    @Transactional(readOnly = true)
    public Page<AlertHistoryResponse> listAll(Pageable pageable) {
        return alertHistoryRepository.findAllByOrderByFiredAtDesc(pageable)
                .map(AlertHistoryResponse::from);
    }

    /**
     * Manually resolve a firing alert.
     * An alert stays unresolved until either:
     *   (a) the job recovers and you call this endpoint, or
     *   (b) it ages out during the nightly cleanup
     */
    @Transactional
    public AlertHistoryResponse resolve(UUID alertId) {
        AlertHistory alert = alertHistoryRepository.findById(alertId)
                .orElseThrow(() -> new JobNotFoundException("Alert not found: " + alertId));

        if (alert.getResolved()) {
            return AlertHistoryResponse.from(alert);   // already resolved, no-op
        }

        alert.setResolved(true);
        alert.setResolvedAt(Instant.now());
        alertHistoryRepository.save(alert);

        log.info("Alert resolved manually: alertId={}, jobId={}", alertId, alert.getJobId());
        return AlertHistoryResponse.from(alert);
    }

    /**
     * Nightly cleanup — called by MonitorEngine scheduler.
     * Deletes resolved alerts older than retentionDays.
     */
    @Transactional
    public void purgeOldAlerts(int retentionDays) {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = alertHistoryRepository.deleteOlderThan(cutoff);
        log.info("Purged {} resolved alert history rows older than {} days", deleted, retentionDays);
    }
}
