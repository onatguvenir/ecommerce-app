package com.monat.ecommerce.common.exception;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends BusinessException {
    
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", 400);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
