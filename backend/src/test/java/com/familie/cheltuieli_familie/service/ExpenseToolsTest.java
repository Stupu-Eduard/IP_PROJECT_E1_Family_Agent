package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseToolsTest {

    @Mock
    private ExpenseAnalyticsService analyticsService;

    @Mock
    private com.familie.cheltuieli_familie.repository.CategoryRepository categoryRepository;

    @Mock
    private com.familie.cheltuieli_familie.repository.FamilyMemberRepository familyMemberRepository;

    @Mock
    private com.familie.cheltuieli_familie.repository.ExpenseItemRepository expenseItemRepository;

    @InjectMocks
    private ExpenseTools expenseTools;

    @Test
    void testCalculateTotal() {
        when(analyticsService.calculateTotal(eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(new BigDecimal("500.00"));

        String result = expenseTools.calculateTotal("2024-01-01", "2024-01-31");

        assertEquals("Total expenses: 500.00 RON", result);
    }

    @Test
    void testCalculateTotalError() {
        when(analyticsService.calculateTotal(eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.calculateTotal("2024-01-01", "2024-01-31");

        assertEquals("Error calculating total: fail", result);
    }

    @Test
    void testCompareMembers() {
        when(analyticsService.compareMembers(eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(Map.of("Teodor", new BigDecimal("300.00"), "Maria", new BigDecimal("200.00")));

        String result = expenseTools.compareMembers("2024-01-01", "2024-01-31");

        assertTrue(result.contains("Teodor: 300.00 RON"));
        assertTrue(result.contains("Maria: 200.00 RON"));
    }

    @Test
    void testCompareMembersEmpty() {
        when(analyticsService.compareMembers(eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(Collections.emptyMap());

        String result = expenseTools.compareMembers("2024-01-01", "2024-01-31");

        assertEquals("No spending data found for the specified period.", result);
    }

    @Test
    void testCompareMembersError() {
        when(analyticsService.compareMembers(eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.compareMembers("2024-01-01", "2024-01-31");

        assertEquals("Error comparing members: fail", result);
    }

    @Test
    void testDetectAnomalies() {
        Map<String, Object> expense = Map.of(
                "amount", new BigDecimal("500.00"),
                "category", "Electronics",
                "date", LocalDate.of(2024, 1, 15)
        );

        when(analyticsService.detectAnomalies(eq(new BigDecimal("200")), any(), any())).thenReturn(List.of(expense));

        String result = expenseTools.detectAnomalies("200");

        assertTrue(result.contains("Electronics"));
        assertTrue(result.contains("500.00 RON"));
    }

    @Test
    void testDetectAnomaliesEmpty() {
        when(analyticsService.detectAnomalies(eq(new BigDecimal("200")), any(), any()))
                .thenReturn(Collections.emptyList());

        String result = expenseTools.detectAnomalies("200");

        assertEquals("No anomalies found above 200 RON.", result);
    }

    @Test
    void testDetectAnomaliesError() {
        when(analyticsService.detectAnomalies(eq(new BigDecimal("200")), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.detectAnomalies("200");

        assertEquals("Error detecting anomalies: fail", result);
    }

    @Test
    void testByCategory() {
        when(analyticsService.byCategory(eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(Map.of("Food", new BigDecimal("300.00"), "Transport", new BigDecimal("100.00")));

        String result = expenseTools.byCategory("2024-01-01", "2024-01-31");

        assertTrue(result.contains("Food: 300.00 RON"));
        assertTrue(result.contains("Transport: 100.00 RON"));
    }

    @Test
    void testByCategoryEmpty() {
        when(analyticsService.byCategory(eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(Collections.emptyMap());

        String result = expenseTools.byCategory("2024-01-01", "2024-01-31");

        assertEquals("No expenses found for the specified period.", result);
    }

    @Test
    void testByCategoryError() {
        when(analyticsService.byCategory(eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.byCategory("2024-01-01", "2024-01-31");

        assertEquals("Error getting category breakdown: fail", result);
    }

    @Test
    void testByPerson() {
        Map<String, Object> expense = Map.of(
                "amount", new BigDecimal("150.00"),
                "category", "Food",
                "date", LocalDate.of(2024, 1, 10)
        );

        when(analyticsService.findByPerson(eq("Teodor"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(List.of(expense));

        String result = expenseTools.byPerson("Teodor", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("Teodor"));
        assertTrue(result.contains("150.00 RON"));
    }

    @Test
    void testByPersonEmpty() {
        when(analyticsService.findByPerson(eq("Teodor"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(Collections.emptyList());

        String result = expenseTools.byPerson("Teodor", "2024-01-01", "2024-01-31");

        assertEquals("No expenses found for Teodor in the specified period.", result);
    }

    @Test
    void testByPersonError() {
        when(analyticsService.findByPerson(eq("Teodor"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.byPerson("Teodor", "2024-01-01", "2024-01-31");

        assertEquals("Error finding expenses for person: fail", result);
    }

    @Test
    void testComparePeriods() {
        when(analyticsService.calculateTotal(eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(new BigDecimal("500.00"));
        when(analyticsService.calculateTotal(eq(LocalDate.of(2024, 2, 1)), eq(LocalDate.of(2024, 2, 29)), any(), any()))
                .thenReturn(new BigDecimal("600.00"));

        String result = expenseTools.comparePeriods("2024-01-01", "2024-01-31", "2024-02-01", "2024-02-29");

        assertTrue(result.contains("500.00 RON"));
        assertTrue(result.contains("600.00 RON"));
    }

    @Test
    void testComparePeriodsError() {
        when(analyticsService.calculateTotal(eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.comparePeriods("2024-01-01", "2024-01-31", "2024-02-01", "2024-02-29");

        assertEquals("Error comparing periods: fail", result);
    }

    @Test
    void testTopExpenses() {
        Map<String, Object> expense = Map.of(
                "amount", new BigDecimal("400.00"),
                "category", "Electronics",
                "person", "Teodor",
                "date", LocalDate.of(2024, 1, 5)
        );

        when(analyticsService.getTopExpenses(eq(3), any(), any())).thenReturn(List.of(expense));

        String result = expenseTools.topExpenses("3");

        assertTrue(result.contains("400.00 RON"));
        assertTrue(result.contains("Electronics"));
    }

    @Test
    void testTopExpensesEmpty() {
        when(analyticsService.getTopExpenses(eq(3), any(), any()))
                .thenReturn(Collections.emptyList());

        String result = expenseTools.topExpenses("3");

        assertEquals("No expenses found.", result);
    }

    @Test
    void testTopExpensesError() {
        when(analyticsService.getTopExpenses(eq(3), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.topExpenses("3");

        assertEquals("Error getting top expenses: fail", result);
    }

    @Test
    void testMonthlyAverage() {
        when(analyticsService.calculateMonthlyAverage(eq(3), any(), any())).thenReturn(new BigDecimal("450.00"));

        String result = expenseTools.monthlyAverage("3");

        assertEquals("Monthly average for the last 3 months: 450.00 RON", result);
    }

    @Test
    void testMonthlyAverageError() {
        when(analyticsService.calculateMonthlyAverage(eq(3), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.monthlyAverage("3");

        assertEquals("Error calculating monthly average: fail", result);
    }

    @Test
    void testDescribeTrend() {
        when(analyticsService.calculateTrend(eq("Food"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn("Spending on Food increased by 10%");

        String result = expenseTools.describeTrend("Food", "2024-01-01", "2024-01-31");

        assertEquals("Spending on Food increased by 10%", result);
    }

    @Test
    void testDescribeTrendError() {
        when(analyticsService.calculateTrend(eq("Food"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.describeTrend("Food", "2024-01-01", "2024-01-31");

        assertEquals("Error calculating trend: fail", result);
    }

    static Stream<Arguments> visualDescriptionSource() {
        return Stream.of(
            Arguments.of("Spending increased by 15.5%", "Trendul arată o creștere de 15.5% pentru Food"),
            Arguments.of("Spending decreased by 8.2%", "Trendul arată o scădere de 8.2% pentru Food"),
            Arguments.of("Spending remained stable", "Trend stabil pentru Food")
        );
    }

    @ParameterizedTest
    @MethodSource("visualDescriptionSource")
    void testGetVisualDescription(String trend, String expected) {
        when(analyticsService.calculateTrend(eq("Food"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(trend);

        String result = expenseTools.getVisualDescription("Food", "2024-01-01", "2024-01-31");

        assertEquals(expected, result);
    }

    @Test
    void testGetVisualDescriptionError() {
        when(analyticsService.calculateTrend(eq("Food"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.getVisualDescription("Food", "2024-01-01", "2024-01-31");

        assertEquals("Error getting visual description: fail", result);
    }

    @Test
    void testByCategoryDetailed() {
        Map<String, Object> expense = Map.of(
                "amount", new BigDecimal("100.00"),
                "location", "Kaufland",
                "date", LocalDate.of(2024, 1, 10),
                "description", "Groceries"
        );

        when(analyticsService.findByCategory(eq("Food"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(List.of(expense));

        String result = expenseTools.byCategoryDetailed("Food", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("Food"));
        assertTrue(result.contains("100.00 RON"));
        assertTrue(result.contains("Kaufland"));
    }

    @Test
    void testByCategoryDetailedEmpty() {
        when(analyticsService.findByCategory(eq("Food"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(Collections.emptyList());

        String result = expenseTools.byCategoryDetailed("Food", "2024-01-01", "2024-01-31");

        assertEquals("No expenses found for category 'Food' in the specified period.", result);
    }

    @Test
    void testByCategoryDetailedError() {
        when(analyticsService.findByCategory(eq("Food"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.byCategoryDetailed("Food", "2024-01-01", "2024-01-31");

        assertEquals("Error finding category expenses: fail", result);
    }

    @Test
    void testByLocation() {
        Map<String, Object> expense = Map.of(
                "amount", new BigDecimal("50.00"),
                "category", "Transport",
                "date", LocalDate.of(2024, 1, 10),
                "description", "Bus ticket"
        );

        when(analyticsService.findByLocation(eq("Kaufland"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(List.of(expense));

        String result = expenseTools.byLocation("Kaufland", "2024-01-01", "2024-01-31");

        assertTrue(result.contains("Kaufland"));
        assertTrue(result.contains("50.00 RON"));
    }

    @Test
    void testByLocationEmpty() {
        when(analyticsService.findByLocation(eq("Kaufland"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(Collections.emptyList());

        String result = expenseTools.byLocation("Kaufland", "2024-01-01", "2024-01-31");

        assertEquals("No expenses found for location 'Kaufland' in the specified period.", result);
    }

    @Test
    void testByLocationError() {
        when(analyticsService.findByLocation(eq("Kaufland"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.byLocation("Kaufland", "2024-01-01", "2024-01-31");

        assertEquals("Error finding location expenses: fail", result);
    }

    @Test
    void testGetCurrentDate() {
        String result = expenseTools.getCurrentDate();
        assertEquals(LocalDate.now().toString(), result);
    }

    @Test
    void testGetDatabaseSchema() {
        String result = expenseTools.getDatabaseSchema();
        assertNotNull(result);
        assertTrue(result.contains("expenses"));
        assertTrue(result.contains("categories"));
        assertTrue(result.contains("users"));
    }

    @Test
    void testListCategoriesEmpty() {
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        String result = expenseTools.listCategories();

        assertEquals("No categories found.", result);
    }

    @Test
    void testListFamilyMembersEmpty() {
        String result = expenseTools.listFamilyMembers();

        assertEquals("No family members found.", result);
    }

    @Test
    void testGetExpenseItems() {
        com.familie.cheltuieli_familie.model.ExpenseItem item = new com.familie.cheltuieli_familie.model.ExpenseItem();
        item.setItemName("Milk");
        item.setQuantity(new BigDecimal("2"));
        item.setAmount(new BigDecimal("10.00"));

        when(expenseItemRepository.findByExpenseId(1L)).thenReturn(List.of(item));

        String result = expenseTools.getExpenseItems("1");

        assertTrue(result.contains("Milk"));
        assertTrue(result.contains("10.00 RON"));
    }

    @Test
    void testGetExpenseItemsEmpty() {
        when(expenseItemRepository.findByExpenseId(1L)).thenReturn(Collections.emptyList());

        String result = expenseTools.getExpenseItems("1");

        assertTrue(result.contains("Nu am găsit articole"));
    }

    @Test
    void testGetExpenseItemsError() {
        when(expenseItemRepository.findByExpenseId(1L)).thenThrow(new RuntimeException("fail"));

        String result = expenseTools.getExpenseItems("1");

        assertTrue(result.contains("Error getting expense items"));
    }

    @Test
    void testExtractPercentageNull() {
        String result = ReflectionTestUtils.invokeMethod(expenseTools, "extractPercentage", (String) null);
        assertEquals("0", result);
    }

    @Test
    void testExtractPercentageTooLong() {
        StringBuilder sb = new StringBuilder("Spending increased by ");
        while (sb.length() <= 1000) {
            sb.append("a lot ");
        }
        String longTrend = sb.toString();

        when(analyticsService.calculateTrend(eq("Food"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn(longTrend);

        String result = expenseTools.getVisualDescription("Food", "2024-01-01", "2024-01-31");

        assertEquals("Trendul arată o creștere de 0% pentru Food", result);
    }

    @Test
    void testExtractPercentageNoMatch() {
        when(analyticsService.calculateTrend(eq("Food"), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), any(), any()))
                .thenReturn("Spending increased but no percent here");

        String result = expenseTools.getVisualDescription("Food", "2024-01-01", "2024-01-31");

        assertEquals("Trendul arată o creștere de 0% pentru Food", result);
    }

    @Test
    void testSearchByAmount() {
        Map<String, Object> expense = Map.of(
                "amount", new BigDecimal("150.00"),
                "category", "Food",
                "location", "Kaufland",
                "date", LocalDate.of(2024, 1, 10),
                "description", "Groceries"
        );

        when(analyticsService.findByAmount(eq(new BigDecimal("150.00")), any(), any())).thenReturn(List.of(expense));

        String result = expenseTools.searchByAmount("150.00");

        assertTrue(result.contains("150.00 RON"));
        assertTrue(result.contains("Food"));
        assertTrue(result.contains("Kaufland"));
    }

    @Test
    void testSearchByAmountEmpty() {
        when(analyticsService.findByAmount(eq(new BigDecimal("999.99")), any(), any())).thenReturn(List.of());

        String result = expenseTools.searchByAmount("999.99");

        assertEquals("Nu am găsit cheltuieli cu suma de 999.99 RON.", result);
    }

    @Test
    void testSearchByAmountWithRawInput() {
        Map<String, Object> expense = new java.util.HashMap<>();
        expense.put("amount", new BigDecimal("200.00"));
        expense.put("category", "Transport");
        expense.put("location", "OMV");
        expense.put("date", LocalDate.of(2024, 1, 15));
        expense.put("description", "Fuel");
        expense.put("raw_input", "BON FISCAL OMV\nMotorina 50L x 4.00 = 200.00");

        when(analyticsService.findByAmount(eq(new BigDecimal("200.00")), any(), any())).thenReturn(List.of(expense));

        String result = expenseTools.searchByAmount("200.00");

        assertTrue(result.contains("200.00 RON"));
        assertTrue(result.contains("OMV"));
        assertTrue(result.contains("Receipt details"));
    }

    @Test
    void testSearchByAmountError() {
        when(analyticsService.findByAmount(eq(new BigDecimal("100.00")), any(), any()))
                .thenThrow(new RuntimeException("fail"));

        String result = expenseTools.searchByAmount("100.00");

        assertEquals("Error searching by amount: fail", result);
    }
}
