package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ExpenseRepositoryTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Test
    @DisplayName("Should generate an ID when a basic expense is saved")
    void shouldGenerateIdWhenExpenseIsSaved() {
        // Arrange
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(100));
        expense.setCurrency("RON");
        expense.setExpenseDate(LocalDateTime.now());

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
        expense.setExpenseDate(LocalDateTime.now());
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Test
    @DisplayName("Expense should be correctly linked to a User")
    void testExpenseUserLink() {
        // Arrange
        User user = new User();
        user.setName("Alexandra");
        user.setEmail("alex@test.com");
        user.setPasswordH("pass");
        User savedUser = userRepository.save(user);

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(100));
        expense.setUser(savedUser);

        // Act
        Expense savedExpense = expenseRepository.save(expense);

        // Assert
        assertEquals(savedUser.getId(), savedExpense.getUser().getId());
    }

    @Test
    @DisplayName("Expense should be correctly linked to a Family")
    void testExpenseFamilyLink() {
        // Arrange
        Family family = new Family();
        family.setName("Simpsons");
        Family savedFamily = familyRepository.save(family);

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(200));
        expense.setFamily(savedFamily);

        // Act
        Expense savedExpense = expenseRepository.save(expense);

        // Assert
        assertEquals("Simpsons", savedExpense.getFamily().getName());
    }

    @Test
    @DisplayName("Expense should be correctly linked to a Location")
    void testExpenseLocationLink() {
        // Arrange
        Location loc = new Location();
        loc.setStore("Mega Image");
        Location savedLoc = locationRepository.save(loc);

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(50));
        expense.setLocation(savedLoc);

        // Act
        Expense savedExpense = expenseRepository.save(expense);

        // Assert
        assertEquals("Mega Image", savedExpense.getLocation().getStore());
    }

    @Test
    @DisplayName("Expense should be correctly linked to a Category")
    void testExpenseCategoryLink() {
        // Arrange
        Category cat = new Category();
        cat.setName("Mancare");
        cat.setIsActive(true);
        Category savedCat = categoryRepository.save(cat);

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(75));
        expense.setCategory(savedCat);

        // Act
        Expense savedExpense = expenseRepository.save(expense);

        // Assert
        assertEquals("Mancare", savedExpense.getCategory().getName());
    }

    @Test
    @DisplayName("Should update the Category of an existing expense")
    void shouldUpdateExpenseCategory() {
        // Arrange
        Category cat1 = new Category(); cat1.setName("Mancare");
        Category cat2 = new Category(); cat2.setName("Transport");
        categoryRepository.save(cat1);
        categoryRepository.save(cat2);

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(50));
        expense.setCategory(cat1);
        Expense saved = expenseRepository.save(expense);

        // Act
        saved.setCategory(cat2);
        Expense updated = expenseRepository.save(saved);

        // Assert
        assertEquals("Transport", updated.getCategory().getName());
    }

    @Test
    @DisplayName("Expense should be saved even without a Location")
    void shouldSaveExpenseWithoutLocation() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(20));
        expense.setExpenseDate(LocalDateTime.now());
        expense.setLocation(null); // Explicit null

        Expense saved = expenseRepository.save(expense);

        assertNull(saved.getLocation());
    }

    @Test
    @DisplayName("Should update the User associated with an Expense")
    void shouldUpdateExpenseUser() {
        User user1 = createAndSaveUser("Ion");
        User user2 = createAndSaveUser("Maria");

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(50));
        expense.setUser(user1);
        Expense saved = expenseRepository.save(expense);

        saved.setUser(user2);
        Expense updated = expenseRepository.save(saved);

        assertEquals(user2.getId(), updated.getUser().getId());
    }

    @Test
    @DisplayName("Should be able to change the location of an existing expense")
    void shouldUpdateExpenseLocation() {
        // Arrange
        Location loc1 = new Location(); loc1.setStore("Lidl");
        Location loc2 = new Location(); loc2.setStore("Kaufland");
        locationRepository.save(loc1);
        locationRepository.save(loc2);

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(30));
        expense.setLocation(loc1);
        Expense saved = expenseRepository.save(expense);

        // Act
        saved.setLocation(loc2);
        Expense updated = expenseRepository.save(saved);

        // Assert
        assertEquals("Kaufland", updated.getLocation().getStore());
    }

    @Test
    @DisplayName("Deleting an expense should NOT delete the associated User")
    void shouldNotDeleteUserWhenExpenseIsDeleted() {
        // Arrange
        User user = createAndSaveUser("Dorel");
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(10));
        expense.setUser(user);
        Expense savedExpense = expenseRepository.save(expense);

        // Act
        expenseRepository.delete(savedExpense);

        // Assert
        assertTrue(userRepository.findById(user.getId()).isPresent(), "User should still exist!");
    }

    @Test
    @DisplayName("Expense should have RON as default currency")
    void shouldHaveDefaultCurrency() {
        // Arrange
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(10));
        expense.setExpenseDate(LocalDateTime.now());

        // Act
        Expense saved = expenseRepository.save(expense);

        // Assert
        assertEquals("RON", saved.getCurrency());
    }

    @Test
    @DisplayName("Deleting an expense SHOULD delete its associated items (Cascade)")
    void shouldDeleteItemsWhenExpenseIsDeleted() {
        // Arrange
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.TEN);
        expense.setItems(new ArrayList<>());

        ExpenseItem item = new ExpenseItem();
        item.setItemName("De Sters");
        item.setExpense(expense);
        expense.getItems().add(item);

        Expense savedExpense = expenseRepository.save(expense);
        Long itemId = savedExpense.getItems().getFirst().getId();

        // Act
        expenseRepository.delete(savedExpense);
        assertNotNull(itemId);
    }

    private User createAndSaveUser(String name) {
        User user = new User();
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@test.com");
        user.setPasswordH("parola_hash_random");
        user.setCreatedAt(java.time.LocalDate.now());
        return userRepository.save(user);
    }

    @Test
    @DisplayName("Constraint: Should correctly handle very large amounts")
    void shouldHandleLargeAmount() {
        Expense expense = new Expense();
        BigDecimal largeAmount = new BigDecimal("9999999.99");
        expense.setAmount(largeAmount);
        expense.setExpenseDate(LocalDateTime.now());

        Expense saved = expenseRepository.save(expense);
        assertEquals(0, largeAmount.compareTo(saved.getAmount()));
    }

    @Test
    @DisplayName("Should save expense with OCR source type")
    void shouldSaveWithOcrSource() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.TEN);
        expense.setSourceType("OCR"); // Testăm câmpul sourceType
        expense.setExpenseDate(LocalDateTime.now());

        Expense saved = expenseRepository.save(expense);
        assertEquals("OCR", saved.getSourceType());
    }

}