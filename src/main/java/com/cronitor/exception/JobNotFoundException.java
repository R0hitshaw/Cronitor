package com.cronitor.exception;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(UUID id) {
        super("Job not found with id: " + id);
    }

    public JobNotFoundException(String message) {
        super(message);
    }
}
