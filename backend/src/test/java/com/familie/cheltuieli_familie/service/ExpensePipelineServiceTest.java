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
    void testProcessRawInput() throws Exception {
        ExtractionResponse extractionResponse = ExtractionResponse.builder()
                .amount(new BigDecimal("89.00"))
                .category("Altele")
                .location("Mega Image")
                .person("Familie")
                .transactionDate(LocalDate.now())
                .rawInput("Am platit 89 lei la Mega Image")
                .build();

        when(extractionService.process(any(ExtractionRequest.class))).thenReturn(List.of(extractionResponse));

        ExpenseEntity savedEntity = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("89.00"))
                .build();
        when(syncService.syncExpense(any(ExpenseEntity.class))).thenReturn(savedEntity);

        doNothing().when(validationService).validatePersistence(1L);
        
        // Mocking ObjectMapper behavior
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        List<Long> result = pipelineService.processRawInput("Am platit 89 lei la Mega Image");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0));
        verify(extractionService, times(1)).process(any(ExtractionRequest.class));
        verify(syncService, times(1)).syncExpense(any(ExpenseEntity.class));
        verify(validationService, times(1)).validatePersistence(1L);
        verify(thePipeHandler, times(1)).broadcast(anyString());
    }
}
