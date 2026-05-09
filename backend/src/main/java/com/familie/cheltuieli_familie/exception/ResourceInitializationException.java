package com.familie.cheltuieli_familie.exception;

public class ResourceInitializationException extends RuntimeException {
    public ResourceInitializationException(String message) {
        super(message);
    }
    public ResourceInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
