package com.cronitor.security;

import com.cronitor.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Authenticates /api/ping/** requests via X-API-Key header.
 *
 * Cron jobs can't do a JWT login flow, so they use a static API key
 * set once and included in every ping request.
 *
 * The raw key is hashed (SHA-256) before DB lookup — we never store
 * raw keys, only their hashes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    private static final String API_KEY_HEADER = "X-API-Key";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        // Only applies to ping endpoints
        if (!request.getRequestURI().startsWith("/api/ping/")) {
            chain.doFilter(request, response);
            return;
        }

        String rawKey = request.getHeader(API_KEY_HEADER);
        if (rawKey == null) {
            chain.doFilter(request, response);
            return;
        }

        String keyHash = sha256(rawKey);
        var apiKey = apiKeyRepository.findByKeyHashAndIsActiveTrue(keyHash);

        if (apiKey.isPresent()) {
            // Update last_used_at asynchronously — don't slow down the ping
            apiKeyRepository.updateLastUsed(apiKey.get().getId());

            var auth = new UsernamePasswordAuthenticationToken(
                    "api-key:" + apiKey.get().getId(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_API_KEY")));

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated via API key: {}", apiKey.get().getName());
        }

        chain.doFilter(request, response);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
