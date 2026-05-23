package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.ExtractionRequest;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpensePipelineServiceTest {

    @Mock
    private ExtractionService extractionService;

    @Mock
    private SyncService syncService;

    @Mock
    private PipelineValidationService validationService;

    @Mock
    private ThePipeHandler thePipeHandler;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ExpensePipelineService pipelineService;

    @Test
    void processRawInput_shouldReturnEmptyListForDuplicate() {
        when(syncService.existsByIdempotencyKey(anyString())).thenReturn(true);

        List<Long> result = pipelineService.processRawInput("duplicate text");

        assertTrue(result.isEmpty());
        verify(extractionService, never()).process(any());
    }

    @Test
    void processRawInput_shouldProcessAndReturnIds() throws Exception {
        when(syncService.existsByIdempotencyKey(anyString())).thenReturn(false);

        ExtractionResponse extraction = ExtractionResponse.builder()
                .amount(new BigDecimal("100.00"))
                .category("Food")
                .location("Store")
                .person("Alice")
                .transactionDate(LocalDate.now())
                .rawInput("test")
                .build();

        when(extractionService.process(any(ExtractionRequest.class))).thenReturn(List.of(extraction));

        ExpenseEntity saved = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .category("Food")
                .build();

        when(syncService.syncExpense(any(ExpenseEntity.class))).thenReturn(saved);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        doNothing().when(thePipeHandler).broadcast(anyString());

        List<Long> result = pipelineService.processRawInput("new text");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0));
        verify(validationService).validatePersistence(1L);
        verify(thePipeHandler).broadcast(anyString());
    }

    @Test
    void processRawInput_shouldHandleMultipleExtractions() throws Exception {
        when(syncService.existsByIdempotencyKey(anyString())).thenReturn(false);

        ExtractionResponse extraction1 = ExtractionResponse.builder()
                .amount(new BigDecimal("100.00"))
                .category("Food")
                .transactionDate(LocalDate.now())
                .build();

        ExtractionResponse extraction2 = ExtractionResponse.builder()
                .amount(new BigDecimal("200.00"))
                .category("Transport")
                .transactionDate(LocalDate.now())
                .build();

        when(extractionService.process(any(ExtractionRequest.class))).thenReturn(List.of(extraction1, extraction2));

        ExpenseEntity saved1 = ExpenseEntity.builder().id(1L).build();
        ExpenseEntity saved2 = ExpenseEntity.builder().id(2L).build();

        when(syncService.syncExpense(any(ExpenseEntity.class))).thenReturn(saved1).thenReturn(saved2);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        doNothing().when(thePipeHandler).broadcast(anyString());

        List<Long> result = pipelineService.processRawInput("multiple items");

        assertEquals(2, result.size());
    }

    @Test
    void processRawInput_shouldHandleBroadcastFailure() throws Exception {
        when(syncService.existsByIdempotencyKey(anyString())).thenReturn(false);

        ExtractionResponse extraction = ExtractionResponse.builder()
                .amount(new BigDecimal("50.00"))
                .category("Food")
                .transactionDate(LocalDate.now())
                .build();

        when(extractionService.process(any(ExtractionRequest.class))).thenReturn(List.of(extraction));

        ExpenseEntity saved = ExpenseEntity.builder().id(1L).build();
        when(syncService.syncExpense(any(ExpenseEntity.class))).thenReturn(saved);
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON error"));

        List<Long> result = pipelineService.processRawInput("test");

        assertEquals(1, result.size());
        // Should not throw even if broadcast fails
    }

    @Test
    void processRawInput_shouldHandleEmptyExtraction() {
        when(syncService.existsByIdempotencyKey(anyString())).thenReturn(false);
        when(extractionService.process(any(ExtractionRequest.class))).thenReturn(List.of());

        List<Long> result = pipelineService.processRawInput("empty");

        assertTrue(result.isEmpty());
    }
}
