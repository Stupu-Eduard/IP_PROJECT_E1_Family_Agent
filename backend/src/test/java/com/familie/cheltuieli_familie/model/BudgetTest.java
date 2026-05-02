package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class BudgetTest {

    @Test
    @DisplayName("Model: Should correctly set start date")
    void testSetStartDate() {
        Budget budget = new Budget();
        LocalDate start = LocalDate.of(2026, 1, 1);
        budget.setStartDate(start);
        assertEquals(start, budget.getStartDate());
    }

    @Test
    @DisplayName("Model: Should correctly set end date")
    void testSetEndDate() {
        Budget budget = new Budget();
        LocalDate end = LocalDate.of(2026, 1, 31);
        budget.setEndDate(end);
        assertEquals(end, budget.getEndDate());
    }

    @Test
    @DisplayName("Model: Should correctly set the amount")
    void testSetAmount() {
        Budget budget = new Budget();
        BigDecimal amount = new BigDecimal("2000.00");
        budget.setAmount(amount);
        assertEquals(amount, budget.getAmount());
    }

    @Test
    @DisplayName("Model: Should link a Category object")
    void testSetCategory() {
        Budget budget = new Budget();
        Category category = new Category();
        category.setName("Entertainment");
        budget.setCategory(category);
        assertNotNull(budget.getCategory());
    }

    @Test
    @DisplayName("Model: Should link a Family object")
    void testSetFamily() {
        Budget budget = new Budget();
        Family family = new Family();
        family.setName("The Smiths");
        budget.setFamily(family);
        assertNotNull(budget.getFamily());
    }

    @Test
    @DisplayName("Model: Should store and retrieve ID")
    void testSetId() {
        Budget budget = new Budget();
        budget.setId(10L);
        assertEquals(10L, budget.getId());
    }
}