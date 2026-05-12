package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.ExtractionRequest;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.model.Expense;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

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
    void testProcessRawInput_Success() throws Exception {
        ExtractionResponse extractionResponse = ExtractionResponse.builder()
                .amount(new BigDecimal("100.00"))
                .category("Food")
                .location("Kaufland")
                .person("Alice")
                .transactionDate(java.time.LocalDate.of(2024, 3, 15))
                .rawInput("text")
                .build();
        when(extractionService.process(any(ExtractionRequest.class))).thenReturn(List.of(extractionResponse));

        Expense savedExpense = Expense.builder().id(1L).build();
        when(syncService.syncExpense(any(Expense.class))).thenReturn(savedExpense);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        List<Long> result = pipelineService.processRawInput("text");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0));
        verify(thePipeHandler, times(1)).broadcast(anyString());
        verify(validationService, times(1)).validatePersistence(1L);
        verify(syncService, times(1)).syncExpense(any(Expense.class));
    }

    @Test
    void testProcessRawInput_whenBroadcastFails_continuesProcessing() throws Exception {
        ExtractionResponse extractionResponse = ExtractionResponse.builder()
                .amount(new BigDecimal("10.00"))
                .category("Transport")
                .location("Metro")
                .person("Bob")
                .transactionDate(java.time.LocalDate.of(2024, 4, 1))
                .rawInput("text")
                .build();
        when(extractionService.process(any(ExtractionRequest.class))).thenReturn(List.of(extractionResponse));

        Expense savedExpense = Expense.builder().id(2L).build();
        when(syncService.syncExpense(any(Expense.class))).thenReturn(savedExpense);

        // Simulam o exceptie la scrierea JSON-ului pentru broadcast
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON error"));

        List<Long> result = pipelineService.processRawInput("text");

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0));
        // Verificam ca eroarea a fost capturata si procesarea a continuat
        verify(thePipeHandler, never()).broadcast(anyString());
        verify(syncService, times(1)).syncExpense(any(Expense.class));
    }
}
