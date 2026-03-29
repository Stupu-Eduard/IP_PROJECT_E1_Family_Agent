package com.proiect.m3.extraction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class AmountNotFoundException extends RuntimeException {
    public AmountNotFoundException(String message) {
        super(message);
    }
}
