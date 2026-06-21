package com.cronitor.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Handles JWT token generation and validation.
 *
 * Uses HMAC-SHA256 (HS256) — a symmetric algorithm where the same secret
 * is used to sign and verify. Fine for a single-server app. For multi-service
 * architectures you'd use RS256 (asymmetric) so services can verify without
 * knowing the signing secret.
 */
@Component
@Slf4j
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${cronitor.jwt.secret}") String secret,
            @Value("${cronitor.jwt.expiration-ms:86400000}") long expirationMs) {
        // Key must be >= 256 bits for HS256
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generate a signed JWT for the given username.
     * Expires after expirationMs (default: 24 hours).
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extract the username (subject) from a token.
     * Returns null if the token is invalid or expired.
     */
    public String extractUsername(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException e) {
            log.debug("JWT extraction failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate a token — checks signature and expiry.
     */
    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}
