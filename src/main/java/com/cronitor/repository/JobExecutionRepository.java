package com.cronitor.repository;

import com.cronitor.domain.JobExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {

    Page<JobExecution> findAllByJobIdOrderByStartedAtDesc(UUID jobId, Pageable pageable);

    Optional<JobExecution> findByJobIdAndRunId(UUID jobId, String runId);

    Optional<JobExecution> findTopByJobIdAndStatusOrderByStartedAtDesc(UUID jobId, String status);

    @Query("""
        SELECT e FROM JobExecution e
        WHERE e.job.id = :jobId
          AND e.status = 'SUCCESS'
          AND e.durationMs IS NOT NULL
        ORDER BY e.startedAt DESC
        """)
    List<JobExecution> findRecentSuccessfulExecutions(@Param("jobId") UUID jobId, Pageable pageable);

    /**
     * Count executions by job, optionally filtered by status.
     * Pass null for status to count ALL executions regardless of status.
     */
    @Query("""
        SELECT COUNT(e) FROM JobExecution e
        WHERE e.job.id = :jobId
          AND (:status IS NULL OR e.status = :status)
          AND e.startedAt >= :since
        """)
    long countByJobIdAndStatusSince(
            @Param("jobId") UUID jobId,
            @Param("status") String status,
            @Param("since") Instant since);
}
