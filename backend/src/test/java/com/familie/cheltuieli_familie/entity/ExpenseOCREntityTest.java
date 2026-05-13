package com.familie.cheltuieli_familie.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseOCREntityTest {

    @Test
    void noArgsConstructorShouldCreateEmptyEntity() {
        ExpenseOCREntity entity = new ExpenseOCREntity();

        assertNull(entity.getId());
        assertNull(entity.getAmount());
        assertNull(entity.getDescription());
        assertNull(entity.getDate());
        assertNull(entity.getCategoryId());
        assertNull(entity.getLocationId());
        assertNull(entity.getUserId());
        assertNull(entity.getFamilyId());
        assertNull(entity.getCurrency());
        assertNull(entity.getTransactionType());
        assertNull(entity.getSourceType());
    }

    @Test
    void constructorShouldSetMainFields() {
        LocalDateTime date = LocalDateTime.of(2026, 3, 10, 0, 0);

        ExpenseOCREntity entity = new ExpenseOCREntity(
                BigDecimal.valueOf(100.50),
                "Lidl",
                date,
                "RON",
                "EXPENSE",
                "OCR"
        );

        assertEquals(BigDecimal.valueOf(100.50), entity.getAmount());
        assertEquals("Lidl", entity.getDescription());
        assertEquals(date, entity.getDate());
        assertEquals("RON", entity.getCurrency());
        assertEquals("EXPENSE", entity.getTransactionType());
        assertEquals("OCR", entity.getSourceType());
    }

    @Test
    void gettersAndSettersShouldWork() {
        ExpenseOCREntity entity = new ExpenseOCREntity();

        LocalDateTime date = LocalDateTime.of(2026, 3, 10, 12, 30);

        entity.setId(1L);
        entity.setAmount(BigDecimal.valueOf(250.75));
        entity.setDescription("Kaufland");
        entity.setDate(date);
        entity.setCategoryId(2L);
        entity.setLocationId(3L);
        entity.setUserId(4L);
        entity.setFamilyId(5L);
        entity.setCurrency("EUR");
        entity.setTransactionType("INCOME");
        entity.setSourceType("OCR");

        assertEquals(1L, entity.getId());
        assertEquals(BigDecimal.valueOf(250.75), entity.getAmount());
        assertEquals("Kaufland", entity.getDescription());
        assertEquals(date, entity.getDate());
        assertEquals(2L, entity.getCategoryId());
        assertEquals(3L, entity.getLocationId());
        assertEquals(4L, entity.getUserId());
        assertEquals(5L, entity.getFamilyId());
        assertEquals("EUR", entity.getCurrency());
        assertEquals("INCOME", entity.getTransactionType());
        assertEquals("OCR", entity.getSourceType());
    }

    @Test
    void settersShouldAcceptNullValues() {
        ExpenseOCREntity entity = new ExpenseOCREntity(
                BigDecimal.valueOf(100.50),
                "Lidl",
                LocalDateTime.of(2026, 3, 10, 0, 0),
                "RON",
                "EXPENSE",
                "OCR"
        );

        entity.setId(null);
        entity.setAmount(null);
        entity.setDescription(null);
        entity.setDate(null);
        entity.setCategoryId(null);
        entity.setLocationId(null);
        entity.setUserId(null);
        entity.setFamilyId(null);
        entity.setCurrency(null);
        entity.setTransactionType(null);
        entity.setSourceType(null);

        assertNull(entity.getId());
        assertNull(entity.getAmount());
        assertNull(entity.getDescription());
        assertNull(entity.getDate());
        assertNull(entity.getCategoryId());
        assertNull(entity.getLocationId());
        assertNull(entity.getUserId());
        assertNull(entity.getFamilyId());
        assertNull(entity.getCurrency());
        assertNull(entity.getTransactionType());
        assertNull(entity.getSourceType());
    }
}