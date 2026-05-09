package com.familie.cheltuieli_familie.exception;

public class VectorStoreException extends RuntimeException {
    public VectorStoreException(String message) {
        super(message);
    }

    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
