package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseEntityTest {

    @Test
    void testBuilderAndGetters() {
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100.50"))
                .category("Food")
                .location("Kaufland")
                .person("Alice")
                .date(LocalDate.of(2024, 3, 15))
                .rawInput("Test input")
                .createdAt(LocalDateTime.now())
                .build();

        assertEquals(1L, expense.getId());
        assertEquals(new BigDecimal("100.50"), expense.getAmount());
        assertEquals("Food", expense.getCategory());
        assertEquals("Kaufland", expense.getLocation());
        assertEquals("Alice", expense.getPerson());
        assertEquals(LocalDate.of(2024, 3, 15), expense.getDate());
        assertEquals("Test input", expense.getRawInput());
        assertNotNull(expense.getCreatedAt());
    }

    @Test
    void testSetters() {
        ExpenseEntity expense = new ExpenseEntity();
        expense.setId(2L);
        expense.setAmount(new BigDecimal("200.00"));
        expense.setCategory("Transport");
        expense.setLocation("Metro");
        expense.setPerson("Bob");
        expense.setDate(LocalDate.of(2024, 4, 1));
        expense.setRawInput("Raw");

        assertEquals(2L, expense.getId());
        assertEquals(new BigDecimal("200.00"), expense.getAmount());
    }

    @Test
    void testPrePersist() {
        ExpenseEntity expense = new ExpenseEntity();
        assertNull(expense.getCreatedAt());
        
        expense.onCreate();
        
        assertNotNull(expense.getCreatedAt());
    }

    @Test
    void testPrePersistNotOverwrite() {
        LocalDateTime existing = LocalDateTime.of(2024, 1, 1, 12, 0);
        ExpenseEntity expense = new ExpenseEntity();
        expense.setCreatedAt(existing);
        
        expense.onCreate();
        
        assertEquals(existing, expense.getCreatedAt());
    }
}
