package com.cronitor.service;

import com.cronitor.domain.JobExecution;
import com.cronitor.domain.MonitoredJob;
import com.cronitor.dto.PingRequest;
import com.cronitor.exception.JobNotFoundException;
import com.cronitor.repository.JobExecutionRepository;
import com.cronitor.repository.MonitoredJobRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeartbeatServiceTest {

    @Mock MonitoredJobRepository jobRepository;
    @Mock JobExecutionRepository executionRepository;
    @Mock EntityManager entityManager;

    @InjectMocks HeartbeatService heartbeatService;

    private MonitoredJob job;
    private PingRequest pingRequest;

    @BeforeEach
    void setUp() {
        job = MonitoredJob.builder()
                .id(UUID.randomUUID())
                .name("Billing job")
                .slug("billing-job")
                .cronExpression("0 0 2 * * *")   // daily at 02:00
                .gracePeriodSeconds(300)
                .status("PENDING")
                .build();

        pingRequest = new PingRequest();
        pingRequest.setRunId("run-001");
        pingRequest.setMessage("starting");
    }

    @Test
    @DisplayName("START ping creates a new RUNNING execution and updates job status")
    void startPing_createsExecution() {
        when(jobRepository.findBySlug("billing-job")).thenReturn(Optional.of(job));
        when(executionRepository.findByJobIdAndRunId(job.getId(), "run-001")).thenReturn(Optional.empty());
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = heartbeatService.recordStart("billing-job", pingRequest);

        assertThat(response.getStatus()).isEqualTo("RUNNING");
        assertThat(response.getRunId()).isEqualTo("run-001");
        verify(jobRepository).save(argThat(j -> "RUNNING".equals(j.getStatus())));
    }

    @Test
    @DisplayName("Duplicate START ping with same runId returns existing execution (idempotent)")
    void startPing_idempotent() {
        JobExecution existing = JobExecution.builder()
                .id(UUID.randomUUID())
                .job(job)
                .runId("run-001")
                .status("RUNNING")
                .startedAt(Instant.now())
                .build();

        when(jobRepository.findBySlug("billing-job")).thenReturn(Optional.of(job));
        when(executionRepository.findByJobIdAndRunId(job.getId(), "run-001"))
                .thenReturn(Optional.of(existing));

        var response = heartbeatService.recordStart("billing-job", pingRequest);

        assertThat(response.getId()).isEqualTo(existing.getId());
        verify(executionRepository, never()).save(any());   // no new save
    }

    @Test
    @DisplayName("FINISH ping marks execution SUCCESS, computes next_expected_at, sets job HEALTHY")
    void finishPing_updatesJobAndExecution() {
        job.setStatus("RUNNING");
        JobExecution runningExecution = JobExecution.builder()
                .id(UUID.randomUUID())
                .job(job)
                .runId("run-001")
                .status("RUNNING")
                .startedAt(Instant.now().minusSeconds(120))
                .build();

        when(jobRepository.findBySlug("billing-job")).thenReturn(Optional.of(job));
        when(executionRepository.findByJobIdAndRunId(job.getId(), "run-001"))
                .thenReturn(Optional.of(runningExecution));
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = heartbeatService.recordFinish("billing-job", pingRequest);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getDurationMs()).isGreaterThan(0);
        verify(jobRepository).save(argThat(j ->
                "HEALTHY".equals(j.getStatus()) && j.getNextExpectedAt() != null));
    }

    @Test
    @DisplayName("FAIL ping marks execution FAILED and sets job status to FAILED")
    void failPing_marksJobFailed() {
        job.setStatus("RUNNING");
        JobExecution runningExecution = JobExecution.builder()
                .id(UUID.randomUUID()).job(job).runId("run-001")
                .status("RUNNING").startedAt(Instant.now().minusSeconds(60))
                .build();

        when(jobRepository.findBySlug("billing-job")).thenReturn(Optional.of(job));
        when(executionRepository.findByJobIdAndRunId(job.getId(), "run-001"))
                .thenReturn(Optional.of(runningExecution));
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = heartbeatService.recordFail("billing-job", pingRequest);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        verify(jobRepository).save(argThat(j -> "FAILED".equals(j.getStatus())));
    }

    @Test
    @DisplayName("Ping to unknown slug throws JobNotFoundException")
    void unknownSlug_throwsNotFound() {
        when(jobRepository.findBySlug("unknown-job")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> heartbeatService.recordStart("unknown-job", pingRequest))
                .isInstanceOf(JobNotFoundException.class);
    }
}
