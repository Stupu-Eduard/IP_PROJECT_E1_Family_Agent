package com.familie.cheltuieli_familie.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ExtractedExpenseDTOTest {

    @Test
    void testRecordCreation() {
        ExtractedExpenseDTO dto = new ExtractedExpenseDTO(
                new BigDecimal("100.00"),
                "RON",
                "Food",
                LocalDate.of(2024, 3, 15),
                "100 lei la magazin"
        );

        assertEquals(new BigDecimal("100.00"), dto.amount());
        assertEquals("RON", dto.currency());
        assertEquals("Food", dto.category());
        assertEquals(LocalDate.of(2024, 3, 15), dto.transactionDate());
        assertEquals("100 lei la magazin", dto.rawText());
    }

    @Test
    void testRecordEquality() {
        ExtractedExpenseDTO dto1 = new ExtractedExpenseDTO(
                new BigDecimal("100.00"), "RON", "Food", LocalDate.of(2024, 3, 15), "text"
        );
        ExtractedExpenseDTO dto2 = new ExtractedExpenseDTO(
                new BigDecimal("100.00"), "RON", "Food", LocalDate.of(2024, 3, 15), "text"
        );

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }
}
