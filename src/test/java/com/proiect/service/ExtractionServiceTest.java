package com.proiect.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proiect.dto.ExtractionRequest;
import com.proiect.dto.ExtractionResponse;
import com.proiect.model.ExpenseEntityDumitrita;
import com.proiect.repository.ExpenseRepositoryDumitrita;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExtractionServiceTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Mock
    private ExpenseRepositoryDumitrita expenseRepository;

    @InjectMocks
    private ExtractionService extractionService;

    private static final String VALID_JSON_RSP = """
            {
              "amount": 50.5,
              "currency": "RON",
              "category": "transport",
              "location": "UBER",
              "person": "teodor",
              "transactionDate": "2023-11-20"
            }
            """;

    @BeforeEach
    void setUp() {
        // ReflectionTestUtils if needed or relies on @InjectMocks
    }

    @Test
    void testLlmExternalCallExtraction() {
        // Arrange
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText("Am platit 50.5 lei la UBER azi dimineata");

        // Mock the LLM Response explicitly
        Response<AiMessage> mockResponse = Response.from(AiMessage.from(VALID_JSON_RSP));
            
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);
        when(expenseRepository.save(any())).thenReturn(new ExpenseEntityDumitrita());

        // Act
        ExtractionResponse response = extractionService.process(req);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("50.50"), response.getAmount());
        assertEquals("transport", response.getCategory());
        assertEquals("UBER", response.getLocation());

        // Verify the LLM was called successfully (LLM External Call Test)
        verify(chatLanguageModel, times(1)).generate(anyList());
        
        // Verify Postgres Saving logic (PostgreSQL Validation)
        ArgumentCaptor<ExpenseEntityDumitrita> entityCaptor = ArgumentCaptor.forClass(ExpenseEntityDumitrita.class);
        verify(expenseRepository, times(1)).save(entityCaptor.capture());
        
        ExpenseEntityDumitrita savedEntity = entityCaptor.getValue();
        assertEquals(new BigDecimal("50.50"), savedEntity.getAmount());
        assertEquals("transport", savedEntity.getCategory());
    }
}
