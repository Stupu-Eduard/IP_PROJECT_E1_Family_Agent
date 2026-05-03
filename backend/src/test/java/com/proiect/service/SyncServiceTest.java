package com.proiect.service;

import com.proiect.event.ExpenseSyncEvent;
import com.proiect.model.ExpenseEntity;
import com.proiect.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @DisplayName("Should save expense and publish sync event")
    void syncExpense_Success() {
        // Given
        ExpenseEntity input = ExpenseEntity.builder()
                .amount(new BigDecimal("100.00"))
                .category("Groceries")
                .location("Lidl")
                .person("Alex")
                .date(LocalDate.now())
                .rawInput("Groceries at Lidl")
                .build();

        ExpenseEntity saved = ExpenseEntity.builder()
                .id(1L)
                .amount(input.getAmount())
                .category(input.getCategory())
                .location(input.getLocation())
                .person(input.getPerson())
                .date(input.getDate())
                .rawInput(input.getRawInput())
                .build();

        when(jpaRepository.save(any(ExpenseEntity.class))).thenReturn(saved);

        // When
        ExpenseEntity result = syncService.syncExpense(input);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Groceries", result.getCategory());
        
        verify(jpaRepository).save(input);
        
        ArgumentCaptor<ExpenseSyncEvent> eventCaptor = ArgumentCaptor.forClass(ExpenseSyncEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        ExpenseSyncEvent event = eventCaptor.getValue();
        assertEquals(saved, event.getExpense());
        assertEquals(syncService, event.getSource());
    }

    @Test
    @DisplayName("Should set rawInput to category if rawInput is missing during sync")
    void syncExpense_MissingRawInput_SetsFromCategory() {
        // Given
        ExpenseEntity input = ExpenseEntity.builder()
                .amount(new BigDecimal("50.00"))
                .category("Utilities")
                .rawInput(null) // explicitly null
                .build();

        ExpenseEntity saved = ExpenseEntity.builder()
                .id(2L)
                .amount(input.getAmount())
                .category(input.getCategory())
                .rawInput(null)
                .build();

        when(jpaRepository.save(input)).thenReturn(saved);

        // When
        ExpenseEntity result = syncService.syncExpense(input);

        // Then
        assertEquals("Utilities", result.getRawInput());
        verify(eventPublisher).publishEvent(any(ExpenseSyncEvent.class));
    }
}
