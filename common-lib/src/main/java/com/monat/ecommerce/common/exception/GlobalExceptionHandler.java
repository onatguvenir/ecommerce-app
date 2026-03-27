package com.monat.ecommerce.common.exception;

import com.monat.ecommerce.common.dto.ErrorResponse;
import com.monat.ecommerce.common.util.LocalizationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized exception handling across all microservices.
 * 
 * Educational Note:
 * - @RestControllerAdvice: Intercepts exceptions thrown by any @RequestMapping method.
 * - This handler ensures that the UI/Client receives a consistent ErrorResponse 
 *   regardless of which service or layer threw the error.
 * - TraceId: Included in the response to help developers find the exact logs 
 *   in Kibana/Jaeger.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

        private final LocalizationUtils localizationUtils;

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(
                        ResourceNotFoundException ex, HttpServletRequest request) {

                log.error("Resource not found: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .error(localizationUtils.getMessage("generic.not-found"))
                                .message(ex.getMessage())
                                .status(404)
                                .path(request.getRequestURI())
                                .timestamp(LocalDateTime.now())
                                .traceId(UUID.randomUUID().toString())
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(ValidationException.class)
        public ResponseEntity<ErrorResponse> handleValidation(
                        ValidationException ex, HttpServletRequest request) {

                log.error("Validation error: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .error(localizationUtils.getMessage("generic.validation-error"))
                                .message(ex.getMessage())
                                .status(400)
                                .path(request.getRequestURI())
                                .timestamp(LocalDateTime.now())
                                .traceId(UUID.randomUUID().toString())
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ErrorResponse> handleBusinessException(
                        BusinessException ex, HttpServletRequest request) {

                log.error("Business exception: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .error(ex.getErrorCode())
                                .message(localizationUtils.getMessage(ex.getErrorCode(), ex.getMessage()))
                                .status(ex.getHttpStatus())
                                .path(request.getRequestURI())
                                .timestamp(LocalDateTime.now())
                                .traceId(UUID.randomUUID().toString())
                                .build();

                return ResponseEntity.status(ex.getHttpStatus()).body(error);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {

                log.error("Validation failed: {}", ex.getMessage());

                Map<String, String> validationErrors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach(error -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        validationErrors.put(fieldName, errorMessage);
                });

                ErrorResponse error = ErrorResponse.builder()
                                .error(localizationUtils.getMessage("generic.validation-error"))
                                .message(localizationUtils.getMessage("generic.validation-error"))
                                .status(400)
                                .path(request.getRequestURI())
                                .timestamp(LocalDateTime.now())
                                .traceId(UUID.randomUUID().toString())
                                .validationErrors(validationErrors)
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ErrorResponse> handleNoResourceFound(
                        NoResourceFoundException ex,
                        HttpServletRequest request) {

                log.debug("Resource not found: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .error("Not Found")
                                .message(ex.getMessage())
                                .status(404)
                                .path(request.getRequestURI())
                                .timestamp(LocalDateTime.now())
                                .traceId(UUID.randomUUID().toString())
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex, HttpServletRequest request) {

                log.error("Unexpected error occurred", ex);

                ErrorResponse error = ErrorResponse.builder()
                                .error("Internal Server Error")
                                .message(localizationUtils.getMessage("generic.error"))
                                .status(500)
                                .path(request.getRequestURI())
                                .timestamp(LocalDateTime.now())
                                .traceId(UUID.randomUUID().toString())
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
}
