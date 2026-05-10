package com.familie.cheltuieli_familie.exception;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }
    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
