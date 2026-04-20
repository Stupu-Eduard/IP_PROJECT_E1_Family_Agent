package com.proiect.service;

import com.proiect.dto.ExtractionRequest;
import com.proiect.dto.ExtractionResponse;
import com.proiect.model.ExpenseEntity;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.springframework.test.context.ActiveProfiles("test")
class ExtractionServiceTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Mock
    private SyncService syncService;

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

    @Test
    void testLlmExternalCallExtraction() {
        // Arrange
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText("Am platit 50.5 lei la UBER azi dimineata");

        Response<AiMessage> mockResponse = Response.from(AiMessage.from(VALID_JSON_RSP));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);
        when(syncService.syncExpense(any(ExpenseEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ExtractionResponse response = extractionService.process(req);

        // Assert
        assertNotNull(response);
        assertEquals(0, new BigDecimal("50.50").compareTo(response.getAmount()));
        assertEquals("transport", response.getCategory());
        assertEquals("UBER", response.getLocation());

        // Verify the LLM was called
        verify(chatLanguageModel, times(1)).generate(anyList());
        // Verify sync was triggered
        verify(syncService, times(1)).syncExpense(any(ExpenseEntity.class));
    }

    @Test
    void testRetryOnMalformedJson() {
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText("Am platit 100 lei la Mega Image");

        // First two attempts return malformed JSON, third succeeds
        when(chatLanguageModel.generate(anyList()))
                .thenReturn(Response.from(AiMessage.from("not json")))
                .thenReturn(Response.from(AiMessage.from("{invalid")))
                .thenReturn(Response.from(AiMessage.from("""
                        {"amount": 100, "category": "mancare", "location": "Mega Image", "person": "Familie", "transactionDate": "2024-03-15"}
                        """)));

        when(syncService.syncExpense(any(ExpenseEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ExtractionResponse response = extractionService.process(req);

        assertNotNull(response);
        assertEquals(0, new BigDecimal("100.0").compareTo(response.getAmount()));
        assertEquals("mancare", response.getCategory());
        // Verify 3 LLM calls due to retries
        verify(chatLanguageModel, times(3)).generate(anyList());
    }

    @Test
    void testRomanianAmountExpressions() {
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText("Am platit o sută jumate la restaurant");

        Response<AiMessage> mockResponse = Response.from(AiMessage.from("""
                {"amount": "o sută jumate", "category": "mancare", "location": "restaurant", "person": "Familie", "transactionDate": "2024-01-10"}
                """));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);
        when(syncService.syncExpense(any(ExpenseEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ExtractionResponse response = extractionService.process(req);

        assertNotNull(response);
        assertEquals(0, new BigDecimal("150.0").compareTo(response.getAmount()));
    }

    @Test
    void testRelativeDateExtraction() {
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText("Ieri am cumparat paine de 5 lei");

        Response<AiMessage> mockResponse = Response.from(AiMessage.from("""
                {"amount": 5, "category": "mancare", "location": "panemar", "person": "Familie", "transactionDate": "2024-01-10"}
                """));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);
        when(syncService.syncExpense(any(ExpenseEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ExtractionResponse response = extractionService.process(req);

        assertNotNull(response);
        assertEquals(0, new BigDecimal("5.0").compareTo(response.getAmount()));
    }
}
