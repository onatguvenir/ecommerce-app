package com.monat.ecommerce.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to handle Correlation ID for tracing requests across microservices.
 * Checks for an existing X-Correlation-ID header; if missing, generates a new
 * UUID.
 * Adds the ID to MDC for logging and to the response headers.
 */
/**
 * Filter for managing Correlation IDs in requests.
 * <p>
 * A Correlation ID is a unique identifier attached to every request that flows
 * through the system.
 * It allows tracing a request across multiple microservices.
 * </p>
 * 
 * @Component makes this class a Spring Bean, allowing it to be automatically
 *            detected and registered
 *            by Spring's component scanning.
 * 
 *            Extending OncePerRequestFilter ensures that this filter is
 *            executed exactly once per request.
 */
@Component
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_LOG_VAR_NAME = "correlationId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String correlationId = getOrGenerateCorrelationId(request);

        // Add to MDC so logs contain the correlation ID
        MDC.put(CORRELATION_ID_LOG_VAR_NAME, correlationId);

        // Add to response headers so the client/caller implies the trace
        response.addHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            log.trace("Processing request with Correlation ID: {}", correlationId);
            filterChain.doFilter(request, response);
        } finally {
            // Standard cleanup to prevent memory leaks in thread pool reuse scenarios
            MDC.remove(CORRELATION_ID_LOG_VAR_NAME);
        }
    }

    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (StringUtils.isBlank(correlationId)) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
