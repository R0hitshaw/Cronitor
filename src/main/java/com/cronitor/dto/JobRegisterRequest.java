package com.cronitor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class JobRegisterRequest {

    @NotBlank(message = "name is required")
    private String name;

    /**
     * Slug must be URL-safe: lowercase alphanumeric + hyphens only.
     * Example: "nightly-billing-job"
     */
    @NotBlank(message = "slug is required")
    @Pattern(
        regexp = "^[a-z0-9][a-z0-9-]{1,98}[a-z0-9]$",
        message = "slug must be lowercase alphanumeric with hyphens, 3–100 chars"
    )
    private String slug;

    @NotBlank(message = "cronExpression is required")
    private String cronExpression;

    @Min(value = 0, message = "gracePeriodSeconds must be >= 0")
    private int gracePeriodSeconds = 300;
}
