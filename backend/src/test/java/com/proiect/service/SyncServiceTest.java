package com.proiect.service;

import com.proiect.model.ExpenseEntity;
import com.proiect.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private ExpenseJpaRepository jpaRepository;

    @Mock
    private QdrantVectorService qdrantVectorService;

    @InjectMocks
    private SyncService syncService;

    @Test
    void testSyncExpense() {
        ExpenseEntity input = ExpenseEntity.builder()
                .amount(new BigDecimal("150.00"))
                .category("Food")
                .location("Kaufland")
                .person("Familie")
                .date(LocalDate.now())
                .rawInput("Am platit 150 lei")
                .build();

        ExpenseEntity saved = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("150.00"))
                .category("Food")
                .location("Kaufland")
                .person("Familie")
                .date(LocalDate.now())
                .rawInput("Am platit 150 lei")
                .build();

        when(jpaRepository.save(input)).thenReturn(saved);
        doNothing().when(qdrantVectorService).storeExpense(any(ExpenseEntity.class));

        ExpenseEntity result = syncService.syncExpense(input);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(jpaRepository, times(1)).save(input);
        verify(qdrantVectorService, times(1)).storeExpense(saved);
    }

    @Test
    void testSyncExpenseWithNullRawInput() {
        ExpenseEntity input = ExpenseEntity.builder()
                .amount(new BigDecimal("50.00"))
                .category("Transport")
                .build();

        ExpenseEntity saved = ExpenseEntity.builder()
                .id(2L)
                .amount(new BigDecimal("50.00"))
                .category("Transport")
                .rawInput("Transport")
                .build();

        when(jpaRepository.save(input)).thenReturn(saved);
        doNothing().when(qdrantVectorService).storeExpense(any(ExpenseEntity.class));

        ExpenseEntity result = syncService.syncExpense(input);

        assertNotNull(result);
        assertEquals("Transport", result.getRawInput());
        verify(qdrantVectorService, times(1)).storeExpense(saved);
    }
}
