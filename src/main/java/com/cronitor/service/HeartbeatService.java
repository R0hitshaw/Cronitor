package com.cronitor.service;

import com.cronitor.domain.ExecutionEvent;
import com.cronitor.domain.JobExecution;
import com.cronitor.domain.MonitoredJob;
import com.cronitor.dto.ExecutionResponse;
import com.cronitor.dto.PingRequest;
import com.cronitor.exception.JobNotFoundException;
import com.cronitor.repository.JobExecutionRepository;
import com.cronitor.repository.MonitoredJobRepository;
import com.cronitor.util.CronExpressionUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeartbeatService {

    private final MonitoredJobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final EntityManager entityManager;

    // ---------------------------------------------------------------
    // START ping: job has begun a run
    // ---------------------------------------------------------------
    @Transactional
    public ExecutionResponse recordStart(String slug, PingRequest request) {
        MonitoredJob job = findJobBySlug(slug);

        // Idempotency: if run_id already exists for this job, return the existing execution
        if (request.getRunId() != null) {
            var existing = executionRepository.findByJobIdAndRunId(job.getId(), request.getRunId());
            if (existing.isPresent()) {
                log.debug("Duplicate START ping for slug={} runId={} — ignoring", slug, request.getRunId());
                return ExecutionResponse.from(existing.get());
            }
        }

        JobExecution execution = JobExecution.builder()
                .job(job)
                .runId(request.getRunId())
                .status("RUNNING")
                .startedAt(Instant.now())
                .exitMessage(request.getMessage())
                .build();

        // FIX: save first so the execution has a DB-assigned ID before addEvent references it
        executionRepository.save(execution);
        addEvent(execution, "START", request.getMessage());

        // Update job status
        job.setStatus("RUNNING");
        job.setLastPingAt(Instant.now());
        jobRepository.save(job);

        log.info("START ping: slug={}, executionId={}", slug, execution.getId());
        return ExecutionResponse.from(execution);
    }

    // ---------------------------------------------------------------
    // FINISH ping: job completed successfully
    // ---------------------------------------------------------------
    @Transactional
    public ExecutionResponse recordFinish(String slug, PingRequest request) {
        MonitoredJob job = findJobBySlug(slug);
        JobExecution execution = findRunningExecution(job, request.getRunId());

        Instant now = Instant.now();
        execution.setStatus("SUCCESS");
        execution.setFinishedAt(now);
        execution.setDurationMs(now.toEpochMilli() - execution.getStartedAt().toEpochMilli());
        execution.setExitMessage(request.getMessage());

        // FIX: save first (persists synthetic executions too) before addEvent references it
        executionRepository.save(execution);
        addEvent(execution, "FINISH", request.getMessage());

        // Advance the job's next expected run and mark healthy
        Instant nextExpected = CronExpressionUtil.nextAfter(job.getCronExpression(), now);
        job.setStatus("HEALTHY");
        job.setLastPingAt(now);
        job.setNextExpectedAt(nextExpected);
        jobRepository.save(job);

        log.info("FINISH ping: slug={}, durationMs={}, nextExpected={}",
                slug, execution.getDurationMs(), nextExpected);
        return ExecutionResponse.from(execution);
    }

    // ---------------------------------------------------------------
    // FAIL ping: job encountered an error
    // ---------------------------------------------------------------
    @Transactional
    public ExecutionResponse recordFail(String slug, PingRequest request) {
        MonitoredJob job = findJobBySlug(slug);
        JobExecution execution = findRunningExecution(job, request.getRunId());

        Instant now = Instant.now();
        execution.setStatus("FAILED");
        execution.setFinishedAt(now);
        execution.setDurationMs(now.toEpochMilli() - execution.getStartedAt().toEpochMilli());
        execution.setExitMessage(request.getMessage());

        // FIX: save first before addEvent references it
        executionRepository.save(execution);
        addEvent(execution, "FAIL", request.getMessage());

        job.setStatus("FAILED");
        job.setLastPingAt(now);
        jobRepository.save(job);

        log.warn("FAIL ping: slug={}, message={}", slug, request.getMessage());
        return ExecutionResponse.from(execution);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private MonitoredJob findJobBySlug(String slug) {
        return jobRepository.findBySlug(slug)
                .orElseThrow(() -> new JobNotFoundException("No job with slug: " + slug));
    }

    private JobExecution findRunningExecution(MonitoredJob job, String runId) {
        if (runId != null) {
            return executionRepository.findByJobIdAndRunId(job.getId(), runId)
                    .orElseGet(() -> findLatestRunning(job));
        }
        return findLatestRunning(job);
    }

    private JobExecution findLatestRunning(MonitoredJob job) {
        return executionRepository
                .findTopByJobIdAndStatusOrderByStartedAtDesc(job.getId(), "RUNNING")
                .orElseGet(() -> {
                    log.warn("No RUNNING execution found for job slug={}, creating synthetic record", job.getSlug());
                    return JobExecution.builder()
                            .job(job)
                            .status("RUNNING")
                            .startedAt(job.getLastPingAt() != null ? job.getLastPingAt() : Instant.now())
                            .build();
                });
    }

    private void addEvent(JobExecution execution, String type, String message) {
        ExecutionEvent event = ExecutionEvent.builder()
                .execution(execution)
                .eventType(type)
                .message(message)
                .occurredAt(Instant.now())
                .build();
        entityManager.persist(event);
    }
}
