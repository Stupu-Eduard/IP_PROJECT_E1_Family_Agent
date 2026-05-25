package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.ExpenseItemRepository;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.security.util.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseToolsTest {

    @Mock
    private ExpenseAnalyticsService analyticsService;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private ExpenseItemRepository expenseItemRepository;

    @Mock
    private ExpenseJpaRepository expenseJpaRepository;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private ExpenseTools expenseTools;

    @BeforeEach
    void setUp() {
        when(securityService.resolveScope()).thenReturn(new Long[]{1L, 2L});
    }

    @Test
    void getCurrentDate_shouldReturnToday() {
        String result = expenseTools.getCurrentDate();
        assertEquals(LocalDate.now().toString(), result);
    }

    @Test
    void listCategories_shouldReturnCategories() {
        when(analyticsService.findCategoriesUsedByFamily(1L, 2L))
                .thenReturn(List.of("Food", "Transport"));

        String result = expenseTools.listCategories();

        assertTrue(result.contains("Food"));
        assertTrue(result.contains("Transport"));
    }

    @Test
    void listCategories_shouldReturnMessageWhenEmpty() {
        when(analyticsService.findCategoriesUsedByFamily(1L, 2L))
                .thenReturn(Collections.emptyList());

        String result = expenseTools.listCategories();

        assertTrue(result.contains("Nu am găsit categorii"));
    }

    @Test
    void calculateTotal_shouldReturnFormattedTotal() {
        when(analyticsService.calculateTotal(any(), any(), eq(1L), eq(2L)))
                .thenReturn(new BigDecimal("150.50"));

        String result = expenseTools.calculateTotal("2024-01-01", "2024-01-31");

        assertTrue(result.contains("150.50"));
        assertTrue(result.contains("RON"));
    }

    @Test
    void calculateTotal_shouldHandleError() {
        when(analyticsService.calculateTotal(any(), any(), eq(1L), eq(2L)))
                .thenThrow(new RuntimeException("DB error"));

        String result = expenseTools.calculateTotal("2024-01-01", "2024-01-31");

        assertTrue(result.contains("Error"));
    }

    @Test
    void compareMembers_shouldReturnFormattedResult() {
        when(analyticsService.compareMembers(any(), any(), eq(1L), eq(2L)))
                .thenReturn(Map.of("Alice", new BigDecimal("100"), "Bob", new BigDecimal("200")));

        String result = expenseTools.compareMembers("2024-01-01", "2024-01-31");

        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("Bob"));
    }

    @Test
    void compareMembers_shouldReturnMessageWhenEmpty() {
        when(analyticsService.compareMembers(any(), any(), eq(1L), eq(2L)))
                .thenReturn(Collections.emptyMap());

        String result = expenseTools.compareMembers("2024-01-01", "2024-01-31");

        assertTrue(result.contains("No spending data"));
    }

    @Test
    void detectAnomalies_shouldReturnFormattedResult() {
        when(analyticsService.detectAnomalies(any(), eq(1L), eq(2L)))
                .thenReturn(List.of(
                        Map.of("category", "Food", "amount", new BigDecimal("500"), "date", "2024-01-15"),
                        Map.of("category", "Transport", "amount", new BigDecimal("300"), "date", "2024-01-20")
                ));

        String result = expenseTools.detectAnomalies("100");

        assertTrue(result.contains("Food"));
        assertTrue(result.contains("Transport"));
    }

    @Test
    void detectAnomalies_shouldReturnMessageWhenEmpty() {
        when(analyticsService.detectAnomalies(any(), eq(1L), eq(2L)))
                .thenReturn(Collections.emptyList());

        String result = expenseTools.detectAnomalies("1000");

        assertTrue(result.contains("No anomalies found"));
    }

    @Test
    void byCategory_shouldReturnFormattedResult() {
        when(analyticsService.byCategory(any(), any(), eq(1L), eq(2L)))
                .thenReturn(Map.of("Food", new BigDecimal("300"), "Transport", new BigDecimal("150")));

        String result = expenseTools.byCategory("2024-01-01", "2024-01-31");

        assertTrue(result.contains("Food"));
        assertTrue(result.contains("Transport"));
    }

    @Test
    void byPerson_shouldReturnFormattedResult() {
        when(analyticsService.findByPerson(anyString(), any(), any(), eq(1L), eq(2L)))
                .thenReturn(List.of(
                        Map.of("amount", new BigDecimal("100"), "category", "Food", "date", "2024-01-15"),
                        Map.of("amount", new BigDecimal("200"), "category", "Transport", "date", "2024-01-20")
                ));

        String result = expenseTools.byPerson("Alice", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("Food"));
    }

    @Test
    void comparePeriods_shouldReturnComparison() {
        when(analyticsService.calculateTotal(any(), any(), eq(1L), eq(2L)))
                .thenReturn(new BigDecimal("500"))
                .thenReturn(new BigDecimal("600"));

        String result = expenseTools.comparePeriods("2024-01-01", "2024-01-31", "2024-02-01", "2024-02-29");

        assertTrue(result.contains("Period 1"));
        assertTrue(result.contains("Period 2"));
    }

    @Test
    void topExpenses_shouldReturnFormattedResult() {
        when(analyticsService.getTopExpenses(anyInt(), eq(1L), eq(2L)))
                .thenReturn(List.of(
                        Map.of("amount", new BigDecimal("500"), "category", "Electronics", "person", "Alice", "date", "2024-01-15"),
                        Map.of("amount", new BigDecimal("300"), "category", "Food", "person", "Bob", "date", "2024-01-20")
                ));

        String result = expenseTools.topExpenses("5");

        assertTrue(result.contains("Electronics"));
        assertTrue(result.contains("Food"));
    }

    @Test
    void monthlyAverage_shouldReturnFormattedResult() {
        when(analyticsService.calculateMonthlyAverage(anyInt(), eq(1L), eq(2L)))
                .thenReturn(new BigDecimal("450.00"));

        String result = expenseTools.monthlyAverage("3");

        assertTrue(result.contains("450.00"));
        assertTrue(result.contains("RON"));
    }

    @Test
    void describeTrend_shouldReturnTrend() {
        when(analyticsService.calculateTrend(anyString(), any(), any(), eq(1L), eq(2L)))
                .thenReturn("Spending on Food has increased by 10%");

        String result = expenseTools.describeTrend("Food", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("increased"));
    }

    @ParameterizedTest
    @CsvSource({
        "Spending on Food has increased by 15%, creștere",
        "Spending on Food has decreased by 10%, scădere",
        "'Spending on Food is 100 RON. No data for the previous period to compare.', stabil"
    })
    void getVisualDescription_shouldReturnCorrectDescription(String trend, String expected) {
        when(analyticsService.calculateTrend(anyString(), any(), any(), eq(1L), eq(2L)))
                .thenReturn(trend);

        String result = expenseTools.getVisualDescription("Food", "2024-01-01", "2024-01-31");

        assertTrue(result.contains(expected));
    }

    @Test
    void byCategoryDetailed_shouldReturnFormattedResult() {
        when(analyticsService.findByCategory(anyString(), any(), any(), eq(1L), eq(2L)))
                .thenReturn(List.of(
                        Map.of("amount", new BigDecimal("100"), "location", "Store", "date", "2024-01-15", "description", "Test", "raw_input", "receipt text")
                ));

        String result = expenseTools.byCategoryDetailed("Food", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("Store"));
    }

    @Test
    void byLocation_shouldReturnFormattedResult() {
        when(analyticsService.findByLocation(anyString(), any(), any(), eq(1L), eq(2L)))
                .thenReturn(List.of(
                        Map.of("amount", new BigDecimal("50"), "category", "Food", "date", "2024-01-15", "description", "Lunch")
                ));

        String result = expenseTools.byLocation("Store", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("Food"));
    }

    @Test
    void searchByAmount_shouldReturnFormattedResult() {
        when(analyticsService.findByAmount(any(), eq(1L), eq(2L)))
                .thenReturn(List.of(
                        Map.of("category", "Food", "amount", new BigDecimal("99.99"), "location", "Lidl", "date", "2024-01-15", "description", "Groceries", "raw_input", "receipt")
                ));

        String result = expenseTools.searchByAmount("99.99");

        assertTrue(result.contains("99.99"));
        assertTrue(result.contains("Lidl"));
    }

    @Test
    void getExpenseItems_shouldReturnItems() {
        com.familie.cheltuieli_familie.model.ExpenseItem item = new com.familie.cheltuieli_familie.model.ExpenseItem();
        item.setItemName("Milk");
        item.setQuantity(new BigDecimal("2"));
        item.setAmount(new BigDecimal("10.00"));

        when(expenseJpaRepository.findByIdAndFamilyIdOrUserId(anyLong(), eq(1L), eq(2L)))
                .thenReturn(Optional.of(new com.familie.cheltuieli_familie.model.ExpenseEntity()));
        when(expenseItemRepository.findByExpenseId(anyLong()))
                .thenReturn(List.of(item));

        String result = expenseTools.getExpenseItems("1");

        assertTrue(result.contains("Milk"));
    }

    @Test
    void getExpenseItems_shouldReturnUnauthorizedMessage() {
        when(expenseJpaRepository.findByIdAndFamilyIdOrUserId(anyLong(), eq(1L), eq(2L)))
                .thenReturn(Optional.empty());

        String result = expenseTools.getExpenseItems("999");

        assertTrue(result.contains("Nu am găsit"));
    }

    @Test
    void getDatabaseSchema_shouldReturnSchema() {
        String result = expenseTools.getDatabaseSchema();

        assertTrue(result.contains("expenses"));
        assertTrue(result.contains("categories"));
        assertTrue(result.contains("users"));
    }

    @Test
    void listFamilyMembers_shouldReturnMembersWhenFamilyExists() {
        User user = new User();
        user.setId(1L);
        user.setName("Alice");

        com.familie.cheltuieli_familie.model.FamilyMember member = new com.familie.cheltuieli_familie.model.FamilyMember();
        member.setUser(user);

        when(familyMemberRepository.findByFamilyId(1L)).thenReturn(List.of(member));

        String result = expenseTools.listFamilyMembers();

        assertTrue(result.contains("Alice"));
    }

    @Test
    void listFamilyMembers_shouldReturnCurrentUserWhenNoFamily() {
        User user = new User();
        user.setId(2L);
        user.setName("Bob");

        when(securityService.resolveScope()).thenReturn(new Long[]{null, 2L});
        when(securityService.getCurrentUser()).thenReturn(user);

        String result = expenseTools.listFamilyMembers();

        assertTrue(result.contains("Bob"));
    }
}