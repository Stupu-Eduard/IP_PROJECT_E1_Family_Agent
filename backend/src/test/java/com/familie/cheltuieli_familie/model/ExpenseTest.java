package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class ExpenseTest {

    @Test
    @DisplayName("Model: Should have RON as default currency")
    void testDefaultCurrency() {
        Expense expense = new Expense();
        assertEquals("RON", expense.getCurrency());
    }

    @Test
    @DisplayName("Model: Should have manual as default source type")
    void testDefaultSourceType() {
        Expense expense = new Expense();
        assertEquals("manual", expense.getSourceType());
    }

    @Test
    @DisplayName("Model: Should initialize createdAt automatically")
    void testCreatedAtIsNotNull() {
        Expense expense = new Expense();
        assertNotNull(expense.getCreatedAt());
    }

    @Test
    @DisplayName("Model: Should correctly set the amount")
    void testSetAmount() {
        Expense expense = new Expense();
        BigDecimal amount = new BigDecimal("150.00");
        expense.setAmount(amount);
        assertEquals(amount, expense.getAmount());
    }

    @Test
    @DisplayName("Model: Should be able to add items to the list")
    void testItemsListSize() {
        Expense expense = new Expense();
        expense.setItems(new ArrayList<>());
        expense.getItems().add(new ExpenseItem());
        assertEquals(1, expense.getItems().size());
    }

    @Test
    @DisplayName("Model: Should correctly set description")
    void testSetDescription() {
        Expense expense = new Expense();
        expense.setDescription("Factura curent");
        assertEquals("Factura curent", expense.getDescription());
    }

    @Test
    @DisplayName("Model: Should correctly set expense date")
    void testSetExpenseDate() {
        Expense expense = new Expense();
        LocalDateTime now = LocalDateTime.now();
        expense.setExpenseDate(now);
        assertEquals(now, expense.getExpenseDate());
    }

    @Test
    @DisplayName("Model: Should correctly link a Category object")
    void testSetCategory() {
        Expense expense = new Expense();
        Category cat = new Category();
        cat.setName("Utilitati");
        expense.setCategory(cat);
        assertEquals("Utilitati", expense.getCategory().getName());
    }

    @Test
    @DisplayName("Model: Should correctly link a User object")
    void testSetUser() {
        Expense expense = new Expense();
        User user = new User();
        user.setName("Alexandra");
        expense.setUser(user);
        assertEquals("Alexandra", expense.getUser().getName());
    }
}

