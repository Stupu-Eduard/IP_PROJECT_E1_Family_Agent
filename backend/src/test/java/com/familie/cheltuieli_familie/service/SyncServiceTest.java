package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.event.ExpenseSyncEvent;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private ExpenseJpaRepository jpaRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SyncService syncService;

    @Test
    void testSyncExpense() {
        ExpenseEntity input = ExpenseEntity.builder()
                .amount(new BigDecimal("150.00"))
                .category("Food")
                .build();

        ExpenseEntity saved = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("150.00"))
                .category("Food")
                .build();

        when(jpaRepository.save(input)).thenReturn(saved);

        ExpenseEntity result = syncService.syncExpense(input);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(jpaRepository, times(1)).save(input);
        verify(eventPublisher, times(1)).publishEvent(any(ExpenseSyncEvent.class));
    }

    @Test
    void testSyncExpense_WithNullRawInput_SetsCategoryAsRawInput() {
        ExpenseEntity input = ExpenseEntity.builder()
                .amount(new BigDecimal("100.00"))
                .category("Transport")
                .build();

        ExpenseEntity saved = ExpenseEntity.builder()
                .id(2L)
                .amount(new BigDecimal("100.00"))
                .category("Transport")
                .build();

        when(jpaRepository.save(input)).thenReturn(saved);

        ExpenseEntity result = syncService.syncExpense(input);

        assertEquals("Transport", result.getRawInput());
        verify(eventPublisher, times(1)).publishEvent(any(ExpenseSyncEvent.class));
    }

    @Test
    void testSyncExpense_WithExistingRawInput_KeepsRawInput() {
        ExpenseEntity input = ExpenseEntity.builder()
                .amount(new BigDecimal("200.00"))
                .category("Food")
                .rawInput("Custom raw input")
                .build();

        ExpenseEntity saved = ExpenseEntity.builder()
                .id(3L)
                .amount(new BigDecimal("200.00"))
                .category("Food")
                .rawInput("Custom raw input")
                .build();

        when(jpaRepository.save(input)).thenReturn(saved);

        ExpenseEntity result = syncService.syncExpense(input);

        assertEquals("Custom raw input", result.getRawInput());
        verify(eventPublisher, times(1)).publishEvent(any(ExpenseSyncEvent.class));
    }

    @Test
    void testSyncExpense_WithNullRawInputAndNullCategory() {
        ExpenseEntity input = ExpenseEntity.builder()
                .amount(new BigDecimal("50.00"))
                .build();

        ExpenseEntity saved = ExpenseEntity.builder()
                .id(4L)
                .amount(new BigDecimal("50.00"))
                .build();

        when(jpaRepository.save(input)).thenReturn(saved);

        ExpenseEntity result = syncService.syncExpense(input);

        assertNull(result.getRawInput());
        verify(eventPublisher, times(1)).publishEvent(any(ExpenseSyncEvent.class));
    }
}
