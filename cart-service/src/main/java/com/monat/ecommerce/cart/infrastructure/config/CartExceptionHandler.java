package com.monat.ecommerce.cart.infrastructure.config;

import com.monat.ecommerce.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cart-service specific exception handler.
 *
 * <p>Handles {@link CartLockService.CartLockException} separately from the
 * {@code GlobalExceptionHandler} in {@code common-lib} because:
 * <ul>
 *   <li>{@code CartLockException} lives in cart-service; {@code common-lib} must not
 *       depend on a specific service module.</li>
 *   <li>HTTP 429 semantics only apply here — other services don't have cart locks.</li>
 * </ul>
 *
 * <p>Spring picks the most specific handler for each exception, so the fallback
 * {@code Exception.class} handler in {@code GlobalExceptionHandler} will still
 * catch everything else.
 */
@Slf4j
@RestControllerAdvice
public class CartExceptionHandler {

    /**
     * Maps {@link CartLockService.CartLockException} → HTTP 429 Too Many Requests.
     *
     * <p>429 semantics: The server understood the request but is refusing to process it
     * because the user has sent too many requests. Clients SHOULD retry after a short
     * delay. We include a {@code Retry-After: 1} header as a hint.
     */
    @ExceptionHandler(CartLockService.CartLockException.class)
    public ResponseEntity<ErrorResponse> handleCartLock(
            CartLockService.CartLockException ex,
            HttpServletRequest request) {

        log.warn("Cart write lock could not be acquired: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .error("Cart Conflict")
                .message("The cart is currently being modified. Please retry in a moment.")
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .traceId(UUID.randomUUID().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "1")         // hint to client: retry after 1 second
                .body(error);
    }
}
