package com.familie.cheltuieli_familie.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ManualExpenseRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validRequest_noViolations() {
        ManualExpenseRequest request = new ManualExpenseRequest();
        request.setAmount(BigDecimal.valueOf(100.00));
        request.setCurrency("RON");
        request.setCategory("mancare");
        request.setDate(LocalDateTime.now());

        Set<ConstraintViolation<ManualExpenseRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void nullAmount_causesViolation() {
        ManualExpenseRequest request = new ManualExpenseRequest();
        request.setAmount(null);
        request.setCurrency("RON");
        request.setCategory("mancare");
        request.setDate(LocalDateTime.now());

        Set<ConstraintViolation<ManualExpenseRequest>> violations = validator.validate(request);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void amountZero_causesViolation() {
        ManualExpenseRequest request = new ManualExpenseRequest();
        request.setAmount(BigDecimal.ZERO);
        request.setCurrency("RON");
        request.setCategory("mancare");
        request.setDate(LocalDateTime.now());

        Set<ConstraintViolation<ManualExpenseRequest>> violations = validator.validate(request);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void blankCurrency_causesViolation() {
        ManualExpenseRequest request = new ManualExpenseRequest();
        request.setAmount(BigDecimal.valueOf(50.00));
        request.setCurrency("");
        request.setCategory("mancare");
        request.setDate(LocalDateTime.now());

        Set<ConstraintViolation<ManualExpenseRequest>> violations = validator.validate(request);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("currency")));
    }

    @Test
    void blankCategory_causesViolation() {
        ManualExpenseRequest request = new ManualExpenseRequest();
        request.setAmount(BigDecimal.valueOf(50.00));
        request.setCurrency("RON");
        request.setCategory("  ");
        request.setDate(LocalDateTime.now());

        Set<ConstraintViolation<ManualExpenseRequest>> violations = validator.validate(request);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("category")));
    }

    @Test
    void nullDate_causesViolation() {
        ManualExpenseRequest request = new ManualExpenseRequest();
        request.setAmount(BigDecimal.valueOf(50.00));
        request.setCurrency("RON");
        request.setCategory("mancare");
        request.setDate(null);

        Set<ConstraintViolation<ManualExpenseRequest>> violations = validator.validate(request);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("date")));
    }

    @Test
    void optionalLocationName_canBeNull() {
        ManualExpenseRequest request = new ManualExpenseRequest();
        request.setAmount(BigDecimal.valueOf(50.00));
        request.setCurrency("RON");
        request.setCategory("mancare");
        request.setDate(LocalDateTime.now());
        request.setLocationName(null);

        Set<ConstraintViolation<ManualExpenseRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void gettersAndSetters_workCorrectly() {
        ManualExpenseRequest request = new ManualExpenseRequest();
        LocalDateTime now = LocalDateTime.now();

        request.setAmount(BigDecimal.valueOf(123.45));
        request.setCurrency("EUR");
        request.setCategory("transport");
        request.setDescription("Uber");
        request.setDate(now);
        request.setLocationName("Centru");

        assertEquals(BigDecimal.valueOf(123.45), request.getAmount());
        assertEquals("EUR", request.getCurrency());
        assertEquals("transport", request.getCategory());
        assertEquals("Uber", request.getDescription());
        assertEquals(now, request.getDate());
        assertEquals("Centru", request.getLocationName());
    }
}