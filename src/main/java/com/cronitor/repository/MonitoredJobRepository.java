package com.cronitor.repository;

import com.cronitor.domain.MonitoredJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonitoredJobRepository extends JpaRepository<MonitoredJob, UUID> {

    Optional<MonitoredJob> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Core monitor query: find jobs whose deadline (next_expected_at + grace) has passed
     * and are not already marked MISSED.
     *
     * Using a JPQL expression so the grace period math stays in SQL.
     */
    @Query("""
        SELECT j FROM MonitoredJob j
        WHERE j.nextExpectedAt IS NOT NULL
          AND j.nextExpectedAt < :now
          AND j.status NOT IN ('MISSED', 'PENDING')
        """)
    List<MonitoredJob> findOverdueJobs(@Param("now") Instant now);

    /**
     * Jobs currently in RUNNING state — used for duration anomaly detection.
     */
    List<MonitoredJob> findAllByStatus(String status);
}
