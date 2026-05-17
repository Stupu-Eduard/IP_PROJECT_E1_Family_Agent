package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.Family;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

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
}
