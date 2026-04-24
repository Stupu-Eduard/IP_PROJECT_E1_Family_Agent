package com.proiect.service;

import com.proiect.event.ExpenseSyncEvent;
import com.proiect.model.ExpenseEntity;
import com.proiect.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
