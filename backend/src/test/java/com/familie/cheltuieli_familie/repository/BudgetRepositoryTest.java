package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Budget;
import com.familie.cheltuieli_familie.model.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class BudgetRepositoryTest {

    @Autowired
    private BudgetRepository budgetRepository;

    @Test
    @DisplayName("Repo: Should generate an ID when saving a budget")
    void testSaveBudgetGeneratesId() {
        Budget budget = createBaseBudget();
        Budget saved = budgetRepository.save(budget);
        assertNotNull(saved.getId());
    }

    @Test
    @DisplayName("Repo: Should persist the correct amount")
    void testSavedAmountIsCorrect() {
        BigDecimal amount = new BigDecimal("1234.56");
        Budget budget = createBaseBudget();
        budget.setAmount(amount);

        Budget saved = budgetRepository.save(budget);
        assertEquals(0, amount.compareTo(saved.getAmount()));
    }

    @Test
    @DisplayName("Repo: Should retrieve budget by ID")
    void testFindBudgetById() {
        Budget saved = budgetRepository.save(createBaseBudget());
        assertTrue(budgetRepository.findById(saved.getId()).isPresent());
    }

    @Test
    @DisplayName("Repo: Should update amount correctly")
    void testUpdateAmount() {
        Budget saved = budgetRepository.save(createBaseBudget());
        BigDecimal newAmount = new BigDecimal("9999.00");
        saved.setAmount(newAmount);
        budgetRepository.save(saved);
        Budget updated = budgetRepository.findById(saved.getId())
                .orElseThrow(() -> new AssertionError("Budget not found by ID"));

        assertEquals(0, newAmount.compareTo(updated.getAmount()));
    }

    @Test
    @DisplayName("Repo: Should link a category to the budget")
    void testBudgetCategoryRelation() {
        Budget budget = createBaseBudget();
        Category category = new Category(); // Assuming Category is persisted elsewhere or handled
        budget.setCategory(category);

        Budget saved = budgetRepository.save(budget);
        assertNotNull(saved.getCategory());
    }

    @Test
    @DisplayName("Repo: Should correctly save the end date")
    void testSavedEndDate() {
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        Budget budget = createBaseBudget();
        budget.setEndDate(endDate);

        Budget saved = budgetRepository.save(budget);
        assertEquals(endDate, saved.getEndDate());
    }

    @Test
    @DisplayName("Repo: Should return empty Optional for non-existent ID")
    void testFindNonExistentBudget() {
        assertFalse(budgetRepository.findById(-1L).isPresent());
    }

    @Test
    @DisplayName("Repo: Should remove budget from database")
    void testDeleteBudget() {
        Budget saved = budgetRepository.save(createBaseBudget());
        Long id = saved.getId();

        budgetRepository.deleteById(id);
        assertFalse(budgetRepository.existsById(id));
    }

    // Helper method to keep tests clean
    private Budget createBaseBudget() {
        Budget budget = new Budget();
        budget.setAmount(new BigDecimal("1000.00"));
        budget.setStartDate(LocalDate.now());
        budget.setEndDate(LocalDate.now().plusMonths(1));
        return budget;
    }
}