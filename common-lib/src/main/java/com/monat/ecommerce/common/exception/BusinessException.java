package com.monat.ecommerce.common.exception;

import lombok.Getter;

/**
 * Base exception for all business logic exceptions
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.httpStatus = 400;
    }

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 400;
    }

    public BusinessException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BUSINESS_ERROR";
        this.httpStatus = 400;
    }
}
