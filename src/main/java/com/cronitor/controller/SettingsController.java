package com.cronitor.controller;

import com.cronitor.domain.ApiKey;
import com.cronitor.domain.User;
import com.cronitor.repository.ApiKeyRepository;
import com.cronitor.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "API key management and account settings")
public class SettingsController {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------------
    // API Keys
    // ---------------------------------------------------------------

    @GetMapping("/api-keys")
    @Operation(summary = "List all active API keys for the current user")
    public ResponseEntity<List<Map<String, Object>>> listKeys(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        List<Map<String, Object>> result = apiKeyRepository
                .findAllByUserIdAndIsActiveTrue(user.getId())
                .stream()
                .map(k -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", k.getId());
                    map.put("name", k.getName());
                    map.put("createdAt", k.getCreatedAt());
                    map.put("lastUsedAt", k.getLastUsedAt());
                    return map;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api-keys")
    @Operation(summary = "Generate a new API key — raw key shown only once")
    public ResponseEntity<Map<String, Object>> generateKey(
            @RequestBody GenerateKeyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        // Generate a secure random key
        String rawKey = generateSecureKey();
        String keyHash = sha256(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .user(user)
                .name(request.getName())
                .keyHash(keyHash)
                .build();

        apiKeyRepository.save(apiKey);

        // Return the raw key ONCE — never stored, never retrievable again
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", apiKey.getId());
        response.put("name", apiKey.getName());
        response.put("rawKey", rawKey);   // shown only this one time
        response.put("createdAt", apiKey.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api-keys/{id}")
    @Operation(summary = "Revoke an API key")
    public ResponseEntity<Void> revokeKey(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        apiKeyRepository.findById(id).ifPresent(key -> {
            if (key.getUser().getId().equals(user.getId())) {
                key.setActive(false);
                apiKeyRepository.save(key);
            }
        });
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------
    // Account — change password
    // ---------------------------------------------------------------

    @PostMapping("/change-password")
    @Operation(summary = "Change the current user's password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String generateSecureKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return "cron_" + HexFormat.of().formatHex(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Data
    public static class GenerateKeyRequest {
        private String name;
    }

    @Data
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }
}