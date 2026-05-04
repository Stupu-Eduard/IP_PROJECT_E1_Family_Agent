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
                .build();
        when(extractionService.process(any(ExtractionRequest.class))).thenReturn(List.of(extractionResponse));

        ExpenseEntity savedEntity = ExpenseEntity.builder().id(1L).build();
        when(syncService.syncExpense(any(ExpenseEntity.class))).thenReturn(savedEntity);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        List<Long> result = pipelineService.processRawInput("text");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0));
        verify(thePipeHandler, times(1)).broadcast(anyString());
        verify(validationService, times(1)).validatePersistence(1L);
    }

    @Test
    void testProcessRawInput_whenBroadcastFails_continuesProcessing() throws Exception {
        ExtractionResponse extractionResponse = ExtractionResponse.builder()
                .amount(new BigDecimal("10.00"))
                .build();
        when(extractionService.process(any(ExtractionRequest.class))).thenReturn(List.of(extractionResponse));

        ExpenseEntity savedEntity = ExpenseEntity.builder().id(2L).build();
        when(syncService.syncExpense(any(ExpenseEntity.class))).thenReturn(savedEntity);

        // Simulam o exceptie la scrierea JSON-ului pentru broadcast
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON error"));

        List<Long> result = pipelineService.processRawInput("text");

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0));
        // Verificam ca eroarea a fost capturata si procesarea a continuat
        verify(thePipeHandler, never()).broadcast(anyString());
    }
}
