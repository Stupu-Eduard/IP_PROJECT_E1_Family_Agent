package com.familie.cheltuieli_familie.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void extractionRequest_rawTextWithinLimit_isValid() {
        ExtractionRequest request = new ExtractionRequest();
        request.setRawText("Short text");

        Set<ConstraintViolation<ExtractionRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void extractionRequest_rawTextExceedsLimit_isInvalid() {
        ExtractionRequest request = new ExtractionRequest();
        request.setRawText("x".repeat(10001));

        Set<ConstraintViolation<ExtractionRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("10000"));
    }

    @Test
    void extractionRequest_blankRawText_isInvalid() {
        ExtractionRequest request = new ExtractionRequest();
        request.setRawText("  ");

        Set<ConstraintViolation<ExtractionRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void rawInputDTO_withinLimit_isValid() {
        RawInputDTO dto = new RawInputDTO("Valid text");

        Set<ConstraintViolation<RawInputDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void rawInputDTO_exceedsLimit_isInvalid() {
        RawInputDTO dto = new RawInputDTO("x".repeat(10001));

        Set<ConstraintViolation<RawInputDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("10000"));
    }

    @Test
    void rawInputDTO_blank_isInvalid() {
        RawInputDTO dto = new RawInputDTO("  ");

        Set<ConstraintViolation<RawInputDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void ragRequest_queryWithinLimit_isValid() {
        RagRequest request = new RagRequest();
        request.setQuery("Valid query");

        Set<ConstraintViolation<RagRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void ragRequest_queryExceedsLimit_isInvalid() {
        RagRequest request = new RagRequest();
        request.setQuery("x".repeat(10001));

        Set<ConstraintViolation<RagRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("10000"));
    }

    @Test
    void ragRequest_blankQuery_isInvalid() {
        RagRequest request = new RagRequest();
        request.setQuery("  ");

        Set<ConstraintViolation<RagRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
