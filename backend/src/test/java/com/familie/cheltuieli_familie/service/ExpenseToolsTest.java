package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.ExpenseItem;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.repository.ExpenseItemRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.security.util.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseToolsTest {

    @Mock private ExpenseAnalyticsService analyticsService;
    @Mock private CategoryRepository categoryRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private ExpenseItemRepository expenseItemRepository;
    @Mock private SecurityService securityService;

    @InjectMocks
    private ExpenseTools expenseTools;

    @BeforeEach
    void setUp() {
        // No-op: initialization is handled by MockitoExtension
    }

    @Test
    void getCurrentDate_shouldReturnNow() {
        assertEquals(LocalDate.now().toString(), expenseTools.getCurrentDate());
    }

    @Test
    void listCategories_shouldReturnJoinedNames() {
        Category c1 = new Category(); c1.setName("Food");
        Category c2 = new Category(); c2.setName("Car");
        when(categoryRepository.findAll()).thenReturn(List.of(c1, c2));

        String result = expenseTools.listCategories();
        assertTrue(result.contains("Food, Car"));
    }

    @Test
    void listFamilyMembers_shouldReturnNamesFromFamily() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        User u = new User(); u.setName("John");
        FamilyMember fm = new FamilyMember(); fm.setUser(u);
        when(familyMemberRepository.findByFamilyId(anyLong())).thenReturn(List.of(fm));

        String result = expenseTools.listFamilyMembers();
        assertTrue(result.contains("John"));
    }

    @Test
    void calculateTotal_shouldReturnFormattedString() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.calculateTotal(any(), any(), any(), any()))
                .thenReturn(new BigDecimal("500.00"));

        String result = expenseTools.calculateTotal("2026-01-01", "2026-01-31");
        assertEquals("Total expenses: 500.00 RON", result);
    }

    @Test
    void detectAnomalies_shouldReturnFormattedString() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.detectAnomalies(any(), any(), any()))
                .thenReturn(List.of(Map.of("category", "Food", "amount", new BigDecimal("300"), "date", LocalDate.now())));

        String result = expenseTools.detectAnomalies("200");
        assertTrue(result.contains("Food (300 RON on"));
    }

    @Test
    void byCategoryDetailed_shouldReturnFormattedString() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.findByCategory(anyString(), any(), any(), any(), any()))
                .thenReturn(List.of(Map.of(
                        "amount", new BigDecimal("10"),
                        "location", "Store",
                        "date", LocalDate.now(),
                        "description", "Desc",
                        "raw_input", "OCR Text"
                )));

        String result = expenseTools.byCategoryDetailed("Food", "2026-01-01", "2026-01-31");
        assertTrue(result.contains("10 RON at Store"));
        assertTrue(result.contains("Receipt details: OCR Text"));
    }

    @Test
    void compareMembers_shouldReturnFormattedString() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.compareMembers(any(), any(), anyLong(), anyLong()))
                .thenReturn(Map.of("John", new BigDecimal("100"), "Jane", new BigDecimal("200")));

        String result = expenseTools.compareMembers("2026-01-01", "2026-01-31");
        assertTrue(result.contains("John: 100 RON"));
        assertTrue(result.contains("Jane: 200 RON"));
    }

    @Test
    void byCategory_shouldReturnFormattedString() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.byCategory(any(), any(), anyLong(), anyLong()))
                .thenReturn(Map.of("Food", new BigDecimal("300")));

        String result = expenseTools.byCategory("2026-01-01", "2026-01-31");
        assertTrue(result.contains("Food: 300 RON"));
    }

    @Test
    void byPerson_shouldReturnFormattedString() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.findByPerson(anyString(), any(), any(), anyLong(), anyLong()))
                .thenReturn(List.of(Map.of("amount", new BigDecimal("50"), "category", "Misc", "date", LocalDate.now())));

        String result = expenseTools.byPerson("John", "2026-01-01", "2026-01-31");
        assertTrue(result.contains("50 RON for Misc on"));
    }

    @Test
    void comparePeriods_shouldReturnFormattedString() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.calculateTotal(any(), any(), anyLong(), anyLong()))
                .thenReturn(new BigDecimal("100"), new BigDecimal("200"));

        String result = expenseTools.comparePeriods("2026-01-01", "2026-01-15", "2026-01-16", "2026-01-31");
        assertTrue(result.contains("Period 1 (2026-01-01 to 2026-01-15): 100 RON"));
        assertTrue(result.contains("Period 2 (2026-01-16 to 2026-01-31): 200 RON"));
    }

    @Test
    void topExpenses_shouldReturnFormattedString() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.getTopExpenses(anyInt(), anyLong(), anyLong()))
                .thenReturn(List.of(Map.of("amount", new BigDecimal("1000"), "category", "Rent", "person", "John", "date", LocalDate.now())));

        String result = expenseTools.topExpenses("5");
        assertTrue(result.contains("1000 RON (Rent) by John on"));
    }

    @Test
    void monthlyAverage_shouldReturnFormattedString() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.calculateMonthlyAverage(anyInt(), anyLong(), anyLong()))
                .thenReturn(new BigDecimal("1500.50"));

        String result = expenseTools.monthlyAverage("3");
        assertEquals("Monthly average for the last 3 months: 1500.50 RON", result);
    }

    @Test
    void describeTrend_shouldReturnTrendString() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.calculateTrend(anyString(), any(), any(), anyLong(), anyLong()))
                .thenReturn("Trend info");

        String result = expenseTools.describeTrend("Food", "2026-01-01", "2026-01-31");
        assertEquals("Trend info", result);
    }

    @Test
    void getVisualDescription_shouldReturnIncreased() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.calculateTrend(anyString(), any(), any(), anyLong(), anyLong()))
                .thenReturn("increased by 15%");

        String result = expenseTools.getVisualDescription("Food", "2026-01-01", "2026-01-31");
        assertTrue(result.contains("creștere de 15%"));
    }

    @Test
    void getVisualDescription_shouldReturnDecreased() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.calculateTrend(anyString(), any(), any(), anyLong(), anyLong()))
                .thenReturn("decreased by 10%");

        String result = expenseTools.getVisualDescription("Food", "2026-01-01", "2026-01-31");
        assertTrue(result.contains("scădere de 10%"));
    }

    @Test
    void byLocation_shouldReturnFormattedString() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.findByLocation(anyString(), any(), any(), anyLong(), anyLong()))
                .thenReturn(List.of(Map.of("amount", new BigDecimal("20"), "category", "Snack", "date", LocalDate.now(), "description", "chips")));

        String result = expenseTools.byLocation("Shop", "2026-01-01", "2026-01-31");
        assertTrue(result.contains("20 RON for Snack on"));
    }

    @Test
    void searchByAmount_shouldReturnFormattedString() {
        when(securityService.resolveScope()).thenReturn(new Long[]{10L, 1L});
        when(analyticsService.findByAmount(any(), anyLong(), anyLong()))
                .thenReturn(List.of(Map.of(
                        "amount", new BigDecimal("100"),
                        "category", "Gift",
                        "location", "Mall",
                        "date", LocalDate.now(),
                        "description", "present",
                        "raw_input", "receipt text"
                )));

        String result = expenseTools.searchByAmount("100");
        assertTrue(result.contains("Gift - 100 RON la Mall"));
    }

    @Test
    void getExpenseItems_shouldReturnJoinedNames() {
        var mockItem = mock(com.familie.cheltuieli_familie.model.ExpenseItem.class);
        when(mockItem.getItemName()).thenReturn("Milk");
        when(mockItem.getQuantity()).thenReturn(new BigDecimal("2"));
        when(mockItem.getAmount()).thenReturn(new BigDecimal("5.5"));
        
        when(expenseItemRepository.findByExpenseId(anyLong())).thenReturn(List.of(mockItem));

        String result = expenseTools.getExpenseItems("123");
        assertTrue(result.contains("Milk (cantitate: 2, preț: 5.5 RON)"));
    }

    @Test
    void getDatabaseSchema_shouldReturnNonEmptyString() {
        String result = expenseTools.getDatabaseSchema();
        assertFalse(result.isEmpty());
        assertTrue(result.contains("expenses"));
    }

    @Test
    void listFamilyMembers_individualMode_shouldReturnOwnName() {
        when(securityService.resolveScope()).thenReturn(new Long[]{null, 1L});
        User user = new User();
        user.setName("SoloUser");
        when(securityService.getCurrentUser()).thenReturn(user);

        String result = expenseTools.listFamilyMembers();
        assertEquals("Family members: SoloUser", result);
    }
}
