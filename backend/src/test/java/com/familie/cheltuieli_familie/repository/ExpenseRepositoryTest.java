package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.ExpenseItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
class ExpenseRepositoryTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    @DisplayName("Should generate an ID when a basic expense is saved")
    void shouldGenerateIdWhenExpenseIsSaved() {
        // Arrange
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(100));
        expense.setCurrency("RON");
        expense.setExpense_date(LocalDateTime.now());

        // Act
        Expense saved = expenseRepository.save(expense);

        // Assert
        assertNotNull(saved.getId());
    }

    @Test
    @DisplayName("Should persist associated items when an expense is saved")
    void shouldPersistItemsWhenExpenseIsSaved() {
        // Arrange
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(50));
        expense.setExpense_date(LocalDateTime.now());
        expense.setItems(new ArrayList<>());

        ExpenseItem item = new ExpenseItem();
        item.setItemName("Milk");
        item.setAmount(BigDecimal.valueOf(10));
        item.setQuantity(BigDecimal.valueOf(2));
        item.setExpense(expense);

        expense.getItems().add(item);

        // Act
        Expense saved = expenseRepository.save(expense);

        // Assert
        // verify if list is not empty
        assertFalse(saved.getItems().isEmpty());
    }
}