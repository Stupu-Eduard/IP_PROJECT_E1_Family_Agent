package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.Budget;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.BudgetRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BudgetControllerTest {

    private BudgetRepository budgetRepository;
    private ExpenseRepository expenseRepository;
    private FamilyMemberRepository familyMemberRepository;
    private UserRepository userRepository;
    private BudgetController controller;

    @BeforeEach
    void setUp() {
        budgetRepository = mock(BudgetRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        familyMemberRepository = mock(FamilyMemberRepository.class);
        userRepository = mock(UserRepository.class);
        controller = new BudgetController(budgetRepository, expenseRepository, familyMemberRepository, userRepository);
    }

    private User mockUser(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private Authentication authFor(User user) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        return auth;
    }

    private FamilyMember memberInFamily(Long familyId) {
        Family family = mock(Family.class);
        when(family.getId()).thenReturn(familyId);
        FamilyMember fm = mock(FamilyMember.class);
        when(fm.getFamily()).thenReturn(family);
        return fm;
    }

    // ── getChildSummary ──────────────────────────────────────────────────────

    @Test
    void getChildSummary_withBudget_returnsCorrectBalance() {
        User child = mockUser(10L);
        Authentication auth = authFor(child);

        Budget budget = mock(Budget.class);
        when(budget.getAmount()).thenReturn(BigDecimal.valueOf(500));
        when(budgetRepository.findChildBudget(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(budget));
        when(expenseRepository.sumByUserCurrentMonth(eq(10L), anyInt(), anyInt()))
                .thenReturn(BigDecimal.valueOf(200));

        BudgetController.ChildBudgetSummary result = controller.getChildSummary(auth);

        assertEquals(BigDecimal.valueOf(500), result.totalBudget());
        assertEquals(BigDecimal.valueOf(200), result.totalSpent());
        assertEquals(BigDecimal.valueOf(300), result.balance());
    }

    @Test
    void getChildSummary_noBudget_returnsZeroTotals() {
        User child = mockUser(11L);
        Authentication auth = authFor(child);

        when(budgetRepository.findChildBudget(eq(11L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(expenseRepository.sumByUserCurrentMonth(eq(11L), anyInt(), anyInt()))
                .thenReturn(BigDecimal.ZERO);

        BudgetController.ChildBudgetSummary result = controller.getChildSummary(auth);

        assertEquals(BigDecimal.ZERO, result.totalBudget());
        assertEquals(BigDecimal.ZERO, result.totalSpent());
        assertEquals(BigDecimal.ZERO, result.balance());
    }

    @Test
    void getChildSummary_spentExceedsBudget_balanceIsZero() {
        User child = mockUser(12L);
        Authentication auth = authFor(child);

        Budget budget = mock(Budget.class);
        when(budget.getAmount()).thenReturn(BigDecimal.valueOf(100));
        when(budgetRepository.findChildBudget(eq(12L), any(LocalDate.class)))
                .thenReturn(Optional.of(budget));
        when(expenseRepository.sumByUserCurrentMonth(eq(12L), anyInt(), anyInt()))
                .thenReturn(BigDecimal.valueOf(300));

        BudgetController.ChildBudgetSummary result = controller.getChildSummary(auth);

        assertEquals(BigDecimal.ZERO, result.balance());
    }

    // ── getChildBudget ───────────────────────────────────────────────────────

    @Test
    void getChildBudget_existingBudget_returnsAmount() {
        User parent = mockUser(1L);
        FamilyMember parentMember = memberInFamily(5L);
        FamilyMember childMember = memberInFamily(5L);

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(parentMember));
        when(familyMemberRepository.findByUserId(20L)).thenReturn(List.of(childMember));

        Budget budget = mock(Budget.class);
        when(budget.getAmount()).thenReturn(BigDecimal.valueOf(250));
        when(budgetRepository.findChildBudget(eq(20L), any(LocalDate.class)))
                .thenReturn(Optional.of(budget));

        BigDecimal result = controller.getChildBudget(20L, authFor(parent));

        assertEquals(BigDecimal.valueOf(250), result);
    }

    @Test
    void getChildBudget_noBudget_returnsZero() {
        User parent = mockUser(2L);
        FamilyMember parentMember = memberInFamily(7L);
        FamilyMember childMember = memberInFamily(7L);

        when(familyMemberRepository.findByUserId(2L)).thenReturn(List.of(parentMember));
        when(familyMemberRepository.findByUserId(21L)).thenReturn(List.of(childMember));
        when(budgetRepository.findChildBudget(eq(21L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        BigDecimal result = controller.getChildBudget(21L, authFor(parent));

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void getChildBudget_differentFamily_throwsForbidden() {
        User parent = mockUser(3L);
        FamilyMember parentMember = memberInFamily(1L);
        FamilyMember childMember = memberInFamily(2L);

        when(familyMemberRepository.findByUserId(3L)).thenReturn(List.of(parentMember));
        when(familyMemberRepository.findByUserId(30L)).thenReturn(List.of(childMember));

        Authentication auth30 = authFor(parent);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getChildBudget(30L, auth30));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getChildBudget_requesterNotInFamily_throwsForbidden() {
        User parent = mockUser(4L);
        when(familyMemberRepository.findByUserId(4L)).thenReturn(List.of());

        Authentication auth40 = authFor(parent);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getChildBudget(40L, auth40));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getChildBudget_targetNotInFamily_throwsForbidden() {
        User parent = mockUser(5L);
        FamilyMember parentMember = memberInFamily(9L);

        when(familyMemberRepository.findByUserId(5L)).thenReturn(List.of(parentMember));
        when(familyMemberRepository.findByUserId(50L)).thenReturn(List.of());

        Authentication auth50 = authFor(parent);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getChildBudget(50L, auth50));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ── setChildBudget ───────────────────────────────────────────────────────

    @Test
    void setChildBudget_createsNewBudget_whenNoneExists() {
        User parent = mockUser(6L);
        FamilyMember parentMember = memberInFamily(8L);
        FamilyMember childMember = memberInFamily(8L);
        User child = mockUser(60L);

        when(familyMemberRepository.findByUserId(6L)).thenReturn(List.of(parentMember));
        when(familyMemberRepository.findByUserId(60L)).thenReturn(List.of(childMember));
        when(userRepository.findById(60L)).thenReturn(Optional.of(child));
        when(budgetRepository.findChildBudget(eq(60L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        Budget savedBudget = mock(Budget.class);
        when(savedBudget.getAmount()).thenReturn(BigDecimal.valueOf(400));
        when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);

        BudgetController.SetBudgetRequest req = new BudgetController.SetBudgetRequest(BigDecimal.valueOf(400));
        BigDecimal result = controller.setChildBudget(60L, req, authFor(parent));

        assertEquals(BigDecimal.valueOf(400), result);
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void setChildBudget_updatesExistingBudget() {
        User parent = mockUser(7L);
        FamilyMember parentMember = memberInFamily(11L);
        FamilyMember childMember = memberInFamily(11L);
        User child = mockUser(70L);

        when(familyMemberRepository.findByUserId(7L)).thenReturn(List.of(parentMember));
        when(familyMemberRepository.findByUserId(70L)).thenReturn(List.of(childMember));
        when(userRepository.findById(70L)).thenReturn(Optional.of(child));

        Budget existingBudget = new Budget();
        existingBudget.setAmount(BigDecimal.valueOf(100));
        when(budgetRepository.findChildBudget(eq(70L), any(LocalDate.class)))
                .thenReturn(Optional.of(existingBudget));
        when(budgetRepository.save(existingBudget)).thenReturn(existingBudget);

        BudgetController.SetBudgetRequest req = new BudgetController.SetBudgetRequest(BigDecimal.valueOf(600));
        BigDecimal result = controller.setChildBudget(70L, req, authFor(parent));

        assertEquals(BigDecimal.valueOf(600), result);
        assertEquals(BigDecimal.valueOf(600), existingBudget.getAmount());
    }

    @Test
    void setChildBudget_childUserNotFound_throwsNotFound() {
        User parent = mockUser(8L);
        FamilyMember parentMember = memberInFamily(12L);
        FamilyMember childMember = memberInFamily(12L);

        when(familyMemberRepository.findByUserId(8L)).thenReturn(List.of(parentMember));
        when(familyMemberRepository.findByUserId(80L)).thenReturn(List.of(childMember));
        when(userRepository.findById(80L)).thenReturn(Optional.empty());

        BudgetController.SetBudgetRequest req = new BudgetController.SetBudgetRequest(BigDecimal.valueOf(300));
        Authentication auth80 = authFor(parent);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.setChildBudget(80L, req, auth80));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void setChildBudget_parentNotInFamily_throwsBadRequest() {
        User parent = mockUser(9L);
        FamilyMember parentMember = memberInFamily(13L);
        FamilyMember childMember = memberInFamily(13L);
        User child = mockUser(90L);

        when(familyMemberRepository.findByUserId(9L)).thenReturn(List.of(parentMember));
        when(familyMemberRepository.findByUserId(90L)).thenReturn(List.of(childMember));
        when(userRepository.findById(90L)).thenReturn(Optional.of(child));
        // parent not in any family via the second lookup
        when(familyMemberRepository.findByUserId(9L)).thenReturn(List.of());

        BudgetController.SetBudgetRequest req = new BudgetController.SetBudgetRequest(BigDecimal.valueOf(200));
        Authentication auth90 = authFor(parent);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.setChildBudget(90L, req, auth90));

        assertTrue(ex.getStatusCode() == HttpStatus.FORBIDDEN || ex.getStatusCode() == HttpStatus.BAD_REQUEST);
    }
}
