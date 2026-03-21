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
 * Request filter for managing Correlation IDs across microservices.
 * 
 * Educational Note:
 * A Correlation ID (X-Correlation-ID) is a unique token that travels with 
 * a request across multiple microservice hops. It is the backbone of 
 * distributed logging and debugging.
 * 
 * - MDC (Mapped Diagnostic Context): We put the ID here so every log line 
 *   automatically includes it without manual code.
 * - OncePerRequestFilter: Guarantees one execution per container request.
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
