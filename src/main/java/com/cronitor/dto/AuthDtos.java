package com.cronitor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDtos {

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
        private String username;

        @NotBlank
        @Email(message = "Must be a valid email")
        private String email;

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;

        @NotBlank
        private String password;
    }

    @Data
    @lombok.Builder
    public static class AuthResponse {
        private String token;
        private String username;
        private String email;
        private long expiresInMs;
    }
}
