package com.cronitor.service;

import com.cronitor.domain.MonitoredJob;
import com.cronitor.domain.NotificationChannel;
import com.cronitor.dto.JobRegisterRequest;
import com.cronitor.dto.JobResponse;
import com.cronitor.dto.NotificationChannelRequest;
import com.cronitor.dto.NotificationChannelResponse;
import com.cronitor.exception.JobNotFoundException;
import com.cronitor.repository.MonitoredJobRepository;
import com.cronitor.repository.NotificationChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobRegistrationService {

    private final MonitoredJobRepository jobRepository;
    private final NotificationChannelRepository channelRepository;

    @Transactional
    public JobResponse register(JobRegisterRequest request) {
        if (jobRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException(
                    "A job with slug '" + request.getSlug() + "' already exists");
        }

        MonitoredJob job = MonitoredJob.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .cronExpression(request.getCronExpression())
                .gracePeriodSeconds(request.getGracePeriodSeconds())
                .status("PENDING")
                .build();

        MonitoredJob saved = jobRepository.save(job);
        log.info("Registered job: slug={}, id={}", saved.getSlug(), saved.getId());
        return JobResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listAll() {
        return jobRepository.findAll()
                .stream()
                .map(JobResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobResponse getById(UUID id) {
        return jobRepository.findById(id)
                .map(JobResponse::from)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    @Transactional
    public JobResponse update(UUID id, JobRegisterRequest request) {
        MonitoredJob job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        job.setName(request.getName());
        job.setCronExpression(request.getCronExpression());
        job.setGracePeriodSeconds(request.getGracePeriodSeconds());

        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional
    public void delete(UUID id) {
        if (!jobRepository.existsById(id)) {
            throw new JobNotFoundException(id);
        }
        jobRepository.deleteById(id);
        log.info("Deleted job id={}", id);
    }

    // ---------------------------------------------------------------
    // Notification channels
    // ---------------------------------------------------------------

    @Transactional
    public NotificationChannelResponse addChannel(UUID jobId, NotificationChannelRequest request) {
        MonitoredJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        NotificationChannel channel = NotificationChannel.builder()
                .job(job)
                .channelType(request.getChannelType().toUpperCase())
                .configJson(request.getConfigJson())
                .isActive(true)
                .build();

        NotificationChannel saved = channelRepository.save(channel);
        log.info("Added {} channel for job slug={}", saved.getChannelType(), job.getSlug());
        return NotificationChannelResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<NotificationChannelResponse> listChannels(UUID jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new JobNotFoundException(jobId);
        }
        return channelRepository.findAllByJobIdAndIsActiveTrue(jobId)
                .stream()
                .map(NotificationChannelResponse::from)
                .toList();
    }

    @Transactional
    public void removeChannel(UUID id, UUID channelId) {
        if(!jobRepository.existsById(id))
            throw new JobNotFoundException(id);
        channelRepository.deleteById(channelId);
    }
}
