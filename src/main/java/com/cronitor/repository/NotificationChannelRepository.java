package com.cronitor.repository;

import com.cronitor.domain.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, UUID> {

    /**
     * Fetch all active channels for a job.
     * Called by AlertDispatcher when an alert fires.
     */
    List<NotificationChannel> findAllByJobIdAndIsActiveTrue(UUID jobId);
}
