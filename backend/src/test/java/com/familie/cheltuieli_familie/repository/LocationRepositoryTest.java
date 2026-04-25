package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LocationRepositoryTest {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Test
    @DisplayName("Location: Should generate ID upon saving")
    void shouldGenerateId() {
        Location loc = new Location();
        loc.setStore("Auchan");
        Location saved = locationRepository.save(loc);
        assertNotNull(saved.getId());
    }

    @Test
    @DisplayName("Location: Should correctly save the city name")
    void shouldSaveCity() {
        Location loc = new Location();
        loc.setCity("Bucuresti");
        Location saved = locationRepository.save(loc);
        assertEquals("Bucuresti", saved.getCity());
    }

    @Test
    @DisplayName("Location: Should correctly save the country name")
    void shouldSaveCountry() {
        Location loc = new Location();
        loc.setCountry("Romania");
        Location saved = locationRepository.save(loc);
        assertEquals("Romania", saved.getCountry());
    }

    @Test
    @DisplayName("Location: Should correctly save the store name")
    void shouldSaveStoreName() {
        // Arrange
        Location loc = new Location();
        loc.setStore("Kaufland");

        // Act
        Location saved = locationRepository.save(loc);

        // Assert
        assertEquals("Kaufland", saved.getStore());
    }

    @Test
    @DisplayName("Location: Should correctly update the store name")
    void shouldUpdateStoreName() {
        Location loc = new Location();
        loc.setStore("Old Store");
        Location saved = locationRepository.save(loc);

        saved.setStore("New Store");
        Location updated = locationRepository.save(saved);

        assertEquals("New Store", updated.getStore());
    }

    @Test
    @DisplayName("Location: Should delete location record")
    void shouldDeleteLocation() {
        Location loc = new Location();
        loc.setStore("Temporary Store");
        Location saved = locationRepository.save(loc);

        locationRepository.delete(saved);

        assertTrue(locationRepository.findById(saved.getId()).isEmpty());
    }

    @Test
    @DisplayName("Location: Should find a location by its store name")
    void shouldFindLocationByStore() {
        // Arrange
        Location loc = new Location();
        loc.setStore("Penny");
        locationRepository.save(loc);

        // Act
        boolean found = locationRepository.findAll().stream()
                .anyMatch(l -> "Penny".equals(l.getStore()));

        // Assert
        assertTrue(found);
    }

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    @DisplayName("Location: Deleting a location should set expense location to null or remain safe")
    void shouldHandleLocationDeletionWithExpenses() {
        // Arrange
        Location loc = new Location();
        loc.setStore("Kaufland");
        Location savedLoc = locationRepository.save(loc);

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.TEN);
        expense.setLocation(savedLoc);
        expenseRepository.save(expense);

        // Act
        assertThrows(Exception.class, () -> {
            locationRepository.delete(savedLoc);
            locationRepository.flush();
        });
    }
    @Test
    @DisplayName("Location: Should be correctly linked to Family via Expense")
    void testLocationFamilyRelationship() {
        Family family = new Family();
        family.setName("Popescu");
        Family savedFamily = familyRepository.save(family);

        Location loc = new Location();
        loc.setStore("Penny");
        Location savedLoc = locationRepository.save(loc);

        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(50));
        expense.setCurrency("RON");
        expense.setExpenseDate(LocalDateTime.now());
        expense.setFamily(savedFamily);
        expense.setLocation(savedLoc);

        Expense savedExpense = expenseRepository.save(expense);

        assertNotNull(savedExpense.getFamily());
        assertEquals("Penny", savedExpense.getLocation().getStore());
        assertEquals(savedFamily.getId(), savedExpense.getFamily().getId());
    }
}