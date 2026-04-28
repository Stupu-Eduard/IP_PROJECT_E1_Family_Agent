package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpenseItemTest {

    @Test
    @DisplayName("Model Unit Test: Should initialize with quantity 1")
    void testDefaultQuantity() {
        ExpenseItem item = new ExpenseItem();

        assertEquals(BigDecimal.ONE, item.getQuantity());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly set and get Item Name")
    void testItemNameGetterSetter() {
        ExpenseItem item = new ExpenseItem();
        item.setItemName("Paine");
        assertEquals("Paine", item.getItemName());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly set and get amount")
    void testAmountGetterSetter() {
        ExpenseItem item = new ExpenseItem();
        BigDecimal price = new BigDecimal("15.50");
        item.setAmount(price);
        assertEquals(price, item.getAmount());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly link to parent Expense")
    void testExpenseLink() {
        ExpenseItem item = new ExpenseItem();
        Expense expense = new Expense();
        expense.setDescription("Shopping");

        item.setExpense(expense);

        assertEquals("Shopping", item.getExpense().getDescription());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly link to a Category")
    void testCategoryLink() {
        ExpenseItem item = new ExpenseItem();
        Category cat = new Category();
        cat.setName("Food");

        item.setCategory(cat);

        assertEquals("Food", item.getCategory().getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should override default quantity")
    void testSetQuantity() {
        ExpenseItem item = new ExpenseItem();
        BigDecimal newQty = new BigDecimal("5");

        item.setQuantity(newQty);

        assertEquals(newQty, item.getQuantity());
    }
}

