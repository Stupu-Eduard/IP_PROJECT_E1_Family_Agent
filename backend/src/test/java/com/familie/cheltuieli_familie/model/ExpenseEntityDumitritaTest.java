package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ExpenseEntityDumitritaTest {

    @Test
    void testBuilderAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        ExpenseEntityDumitrita entity = ExpenseEntityDumitrita.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .category("food")
                .location("Lidl")
                .person("Eu")
                .transactionDate(now)
                .rawInput("Paine 100 lei")
                .createdAt(now)
                .build();

        assertEquals(1L, entity.getId());
        assertEquals(new BigDecimal("100.00"), entity.getAmount());
        assertEquals("food", entity.getCategory());
        assertEquals("Lidl", entity.getLocation());
        assertEquals("Eu", entity.getPerson());
        assertEquals(now, entity.getTransactionDate());
        assertEquals("Paine 100 lei", entity.getRawInput());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void testNoArgsConstructorAndSetters() {
        ExpenseEntityDumitrita entity = new ExpenseEntityDumitrita();
        entity.setId(2L);
        entity.setAmount(new BigDecimal("50.00"));
        
        assertEquals(2L, entity.getId());
        assertEquals(new BigDecimal("50.00"), entity.getAmount());
    }
}
