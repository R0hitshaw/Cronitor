package com.cronitor.repository;

import com.cronitor.domain.AlertHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, UUID> {

    Page<AlertHistory> findAllByOrderByFiredAtDesc(Pageable pageable);

    List<AlertHistory> findAllByJobIdAndResolvedFalseOrderByFiredAtDesc(UUID jobId);

    long countByJobIdAndResolvedFalse(UUID jobId);

    /**
     * Count MISSED alerts for a job within a time window.
     * Used by StatsService to report missed run count separately from failed runs.
     */
    @Query("""
        SELECT COUNT(a) FROM AlertHistory a
        WHERE a.job.id = :jobId
          AND a.severity = 'CRITICAL'
          AND a.message LIKE '%missed%'
          AND a.firedAt >= :since
        """)
    long countMissedAlertsSince(@Param("jobId") UUID jobId, @Param("since") Instant since);

    /**
     * Nightly cleanup: delete resolved alerts older than the cutoff.
     * Unresolved alerts are kept regardless of age.
     */
    @Modifying
    @Query("DELETE FROM AlertHistory a WHERE a.firedAt < :cutoff AND a.resolved = true")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
