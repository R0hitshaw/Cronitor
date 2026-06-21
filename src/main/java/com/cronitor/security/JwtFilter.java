package com.cronitor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every HTTP request and authenticates it via JWT.
 *
 * Flow:
 * 1. Extract token from Authorization header
 * 2. Validate token signature and expiry
 * 3. Load user from DB by username in token
 * 4. Set user as authenticated in SecurityContext
 * 5. Continue filter chain
 *
 * If any step fails, the request continues unauthenticated.
 * SecurityConfig then decides whether to reject it (401) or allow it.
 *
 * Extends OncePerRequestFilter — guaranteed to run exactly once per
 * request even in complex servlet filter chains.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No token present — continue unauthenticated
        // SecurityConfig will handle whether this is allowed or not
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);  // strip "Bearer " prefix

        if (!jwtUtil.isValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        String username = jwtUtil.extractUsername(token);

        // Only set auth if not already authenticated in this request
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var userDetails = userDetailsService.loadUserByUsername(username);

            var authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("Authenticated user={} via JWT", username);
        }

        chain.doFilter(request, response);
    }
}
