package com.cronitor.service;

import com.cronitor.alert.AlertDispatcher;
import com.cronitor.alert.AlertEvent;
import com.cronitor.domain.JobExecution;
import com.cronitor.domain.MonitoredJob;
import com.cronitor.repository.JobExecutionRepository;
import com.cronitor.repository.MonitoredJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorEngineTest {

    @Mock MonitoredJobRepository jobRepository;
    @Mock JobExecutionRepository executionRepository;
    @Mock AlertDispatcher alertDispatcher;

    @InjectMocks MonitorEngine monitorEngine;

    private MonitoredJob healthyJob;

    @BeforeEach
    void setUp() {
        healthyJob = MonitoredJob.builder()
                .id(UUID.randomUUID())
                .name("Billing job")
                .slug("billing-job")
                .cronExpression("0 0 2 * * *")
                .gracePeriodSeconds(300)
                .status("HEALTHY")
                .build();
    }

    // ---------------------------------------------------------------
    // Missed run tests
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Job past grace period is marked MISSED and alert is dispatched")
    void missedRun_pastGracePeriod_firesAlert() {
        // next_expected_at = 10 minutes ago, grace = 5 min → definitely missed
        healthyJob.setNextExpectedAt(Instant.now().minusSeconds(600));
        healthyJob.setGracePeriodSeconds(300);

        when(jobRepository.findOverdueJobs(any())).thenReturn(List.of(healthyJob));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        monitorEngine.checkMissedRuns();

        // Job status updated to MISSED
        verify(jobRepository).save(argThat(j -> "MISSED".equals(j.getStatus())));

        // Alert dispatched with correct type
        ArgumentCaptor<AlertEvent> captor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(alertDispatcher).dispatch(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(AlertEvent.Type.MISSED);
        assertThat(captor.getValue().getJobSlug()).isEqualTo("billing-job");
    }

    @Test
    @DisplayName("Job within grace period is NOT marked MISSED yet")
    void missedRun_withinGracePeriod_noAlert() {
        // next_expected_at = 2 minutes ago, grace = 5 min → still within grace
        healthyJob.setNextExpectedAt(Instant.now().minusSeconds(120));
        healthyJob.setGracePeriodSeconds(300);

        when(jobRepository.findOverdueJobs(any())).thenReturn(List.of(healthyJob));

        monitorEngine.checkMissedRuns();

        // No status update, no alert
        verify(jobRepository, never()).save(any());
        verify(alertDispatcher, never()).dispatch(any());
    }

    @Test
    @DisplayName("No overdue jobs means no alerts fired")
    void noOverdueJobs_noAlertsDispatched() {
        when(jobRepository.findOverdueJobs(any())).thenReturn(List.of());

        monitorEngine.checkMissedRuns();

        verify(alertDispatcher, never()).dispatch(any());
    }

    // ---------------------------------------------------------------
    // Duration anomaly tests
    // ---------------------------------------------------------------

    @Test
    @DisplayName("RUNNING job exceeding 2x average duration fires DURATION_ANOMALY alert")
    void durationAnomaly_exceedsThreshold_firesAlert() {
        // Job has been running for 10 minutes
        healthyJob.setStatus("RUNNING");
        healthyJob.setLastPingAt(Instant.now().minusSeconds(600));

        // Rolling average = 2 minutes (120s, 120_000ms) → 2× = 240s
        // Elapsed = 600s → anomaly
        List<JobExecution> history = buildExecutionHistory(10, 120_000L);

        when(jobRepository.findAllByStatus("RUNNING")).thenReturn(List.of(healthyJob));
        when(executionRepository.findRecentSuccessfulExecutions(eq(healthyJob.getId()), any(Pageable.class)))
                .thenReturn(history);

        monitorEngine.checkDurationAnomalies();

        ArgumentCaptor<AlertEvent> captor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(alertDispatcher).dispatch(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(AlertEvent.Type.DURATION_ANOMALY);
    }

    @Test
    @DisplayName("RUNNING job within normal duration range does NOT fire alert")
    void durationAnomaly_withinNormalRange_noAlert() {
        // Job has been running for 1.5 minutes
        healthyJob.setStatus("RUNNING");
        healthyJob.setLastPingAt(Instant.now().minusSeconds(90));

        // Rolling average = 2 minutes → 2× = 4 min. Elapsed = 1.5 min → fine
        List<JobExecution> history = buildExecutionHistory(10, 120_000L);

        when(jobRepository.findAllByStatus("RUNNING")).thenReturn(List.of(healthyJob));
        when(executionRepository.findRecentSuccessfulExecutions(eq(healthyJob.getId()), any(Pageable.class)))
                .thenReturn(history);

        monitorEngine.checkDurationAnomalies();

        verify(alertDispatcher, never()).dispatch(any());
    }

    @Test
    @DisplayName("Job with fewer than 3 successful runs skips anomaly check (insufficient history)")
    void durationAnomaly_insufficientHistory_skipped() {
        healthyJob.setStatus("RUNNING");
        healthyJob.setLastPingAt(Instant.now().minusSeconds(600));

        // Only 2 runs — not enough to compute a reliable average
        List<JobExecution> history = buildExecutionHistory(2, 120_000L);

        when(jobRepository.findAllByStatus("RUNNING")).thenReturn(List.of(healthyJob));
        when(executionRepository.findRecentSuccessfulExecutions(eq(healthyJob.getId()), any(Pageable.class)))
                .thenReturn(history);

        monitorEngine.checkDurationAnomalies();

        verify(alertDispatcher, never()).dispatch(any());
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private List<JobExecution> buildExecutionHistory(int count, long durationMs) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> JobExecution.builder()
                        .id(UUID.randomUUID())
                        .job(healthyJob)
                        .status("SUCCESS")
                        .durationMs(durationMs)
                        .startedAt(Instant.now().minusSeconds(3600L * (i + 1)))
                        .build())
                .toList();
    }
}
