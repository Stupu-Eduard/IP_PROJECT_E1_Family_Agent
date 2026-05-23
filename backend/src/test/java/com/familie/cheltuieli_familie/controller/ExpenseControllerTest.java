package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.CreateExpenseRequest;
import com.familie.cheltuieli_familie.dto.ExpenseListDto;
import com.familie.cheltuieli_familie.dto.LocationDto;
import com.familie.cheltuieli_familie.mapper.ExpenseMapper;
import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseControllerTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseController expenseController;

    private User createUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("Test User");
        return user;
    }

    private Authentication createAuth(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    }

    private ExpenseListDto createExpenseListDto() {
        return new ExpenseListDto(
                1L, new BigDecimal("100"), "RON", "Test",
                LocalDateTime.now(), "Food", "Test User",
                null, "MANUAL", null
        );
    }

    @Test
    void list_shouldReturnEmptyListWhenNotAuthenticated() {
        List<ExpenseListDto> result = expenseController.list(null, null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void list_shouldReturnUserExpenses() {
        User user = createUser(1L, "test@test.com");
        Authentication auth = createAuth(user);

        when(familyMemberRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(expenseRepository.findAllByUserFiltered(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());

        List<ExpenseListDto> result = expenseController.list(null, null, null, auth);
        assertNotNull(result);
    }

    @Test
    void create_shouldCreateExpense() {
        User user = createUser(1L, "test@test.com");
        Authentication auth = createAuth(user);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setAmount(new BigDecimal("100"));
        request.setDescription("Test");
        request.setDate(LocalDate.now());
        request.setCategoryName("Food");

        Category category = new Category();
        category.setName("Food");

        Expense expense = new Expense();
        expense.setId(1L);

        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);
        when(expenseRepository.findOneWithLocation(1L)).thenReturn(null);
        when(expenseMapper.toDto(any())).thenReturn(createExpenseListDto());

        ExpenseListDto result = expenseController.create(request, auth);
        assertNotNull(result);
    }

    @Test
    void create_shouldThrowWhenCategoryNotFound() {
        User user = createUser(1L, "test@test.com");
        Authentication auth = createAuth(user);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setCategoryName("NonExistent");

        when(categoryRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> expenseController.create(request, auth));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void getById_shouldReturnExpense() {
        ExpenseRepository.ExpenseWithLocationProjection projection = mock(ExpenseRepository.ExpenseWithLocationProjection.class);
        when(expenseRepository.findOneWithLocation(1L)).thenReturn(projection);
        when(expenseMapper.toDto(any())).thenReturn(createExpenseListDto());

        ExpenseListDto result = expenseController.getById(1L);
        assertNotNull(result);
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(expenseRepository.findOneWithLocation(999L)).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> expenseController.getById(999L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void delete_shouldThrowWhenChildUser() {
        User user = createUser(1L, "test@test.com");
        Authentication auth = createAuth(user);

        FamilyMember member = new FamilyMember();
        member.setRole("Child");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(member));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> expenseController.delete(1L, auth));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void delete_shouldDeleteWhenParentUser() {
        User user = createUser(1L, "test@test.com");
        Authentication auth = createAuth(user);

        FamilyMember member = new FamilyMember();
        member.setRole("Parent");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(member));

        Expense expense = new Expense();
        expense.setId(1L);
        expense.setUser(user);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(familyMemberRepository.existsByFamilyIdAndUserId(any(), any())).thenReturn(true);

        assertDoesNotThrow(() -> expenseController.delete(1L, auth));
        verify(expenseRepository).deleteById(1L);
    }
}
