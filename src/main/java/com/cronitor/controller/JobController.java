package com.cronitor.controller;

import com.cronitor.dto.JobRegisterRequest;
import com.cronitor.dto.JobResponse;
import com.cronitor.dto.NotificationChannelRequest;
import com.cronitor.dto.NotificationChannelResponse;
import com.cronitor.service.JobRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Register and manage monitored cron jobs")
public class JobController {

    private final JobRegistrationService registrationService;

    @PostMapping
    @Operation(summary = "Register a new job")
    public ResponseEntity<JobResponse> register(@Valid @RequestBody JobRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    @GetMapping
    @Operation(summary = "List all registered jobs with their current status")
    public ResponseEntity<List<JobResponse>> listAll() {
        return ResponseEntity.ok(registrationService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single job by ID")
    public ResponseEntity<JobResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update job configuration (cron expression, grace period)")
    public ResponseEntity<JobResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody JobRegisterRequest request) {
        return ResponseEntity.ok(registrationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job and all its history")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        registrationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------
    // Notification channels
    // ---------------------------------------------------------------

    @PostMapping("/{id}/channels")
    @Operation(summary = "Add a notification channel to a job (EMAIL, SLACK, SNS)")
    public ResponseEntity<NotificationChannelResponse> addChannel(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationChannelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.addChannel(id, request));
    }

    @GetMapping("/{id}/channels")
    @Operation(summary = "List all active notification channels for a job")
    public ResponseEntity<List<NotificationChannelResponse>> listChannels(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.listChannels(id));
    }
}
