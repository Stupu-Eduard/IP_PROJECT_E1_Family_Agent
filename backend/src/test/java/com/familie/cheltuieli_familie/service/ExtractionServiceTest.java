package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.ExtractionRequest;
import com.familie.cheltuieli_familie.dto.ExtractionResponse;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.util.List;

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
              "expenses": [
                {
                  "amount": 50.5,
                  "currency": "RON",
                  "category": "transport",
                  "location": "UBER",
                  "person": "teodor",
                  "transactionDate": "2023-11-20"
                }
              ]
            }
            """;

    @Test
    void testLlmExternalCallExtraction() {
        // Arrange
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText("Am platit 50.5 lei la UBER azi dimineata");

        Response<AiMessage> mockResponse = Response.from(AiMessage.from(VALID_JSON_RSP));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);

        // Act
        List<ExtractionResponse> responses = extractionService.process(req);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        ExtractionResponse response = responses.get(0);
        assertEquals(0, new BigDecimal("50.50").compareTo(response.getAmount()));
        assertEquals("transport", response.getCategory());
        assertEquals("UBER", response.getLocation());

        // Verify the LLM was called
        verify(chatLanguageModel, times(1)).generate(anyList());
        // Verify sync was NEVER triggered
        verifyNoInteractions(syncService);
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
                        {"expenses": [{"amount": 100, "category": "mancare", "location": "Mega Image", "person": "Familie", "transactionDate": "2024-03-15"}]}
                        """)));

        List<ExtractionResponse> responses = extractionService.process(req);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(0, new BigDecimal("100.0").compareTo(responses.get(0).getAmount()));
        assertEquals("mancare", responses.get(0).getCategory());
        // Verify 3 LLM calls due to retries
        verify(chatLanguageModel, times(3)).generate(anyList());
        verifyNoInteractions(syncService);
    }

    @Test
    void testRomanianAmountExpressions() {
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText("Am platit o sută jumate la restaurant");

        Response<AiMessage> mockResponse = Response.from(AiMessage.from("""
                {"expenses": [{"amount": "o sută jumate", "category": "mancare", "location": "restaurant", "person": "Familie", "transactionDate": "2024-01-10"}]}
                """));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);

        List<ExtractionResponse> responses = extractionService.process(req);

        assertNotNull(responses);
        assertEquals(0, new BigDecimal("150.0").compareTo(responses.get(0).getAmount()));
        verifyNoInteractions(syncService);
    }

    @Test
    void testConsistencyValidation() {
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText("Paine 5 lei, Lapte 10 lei. Total 15 lei.");

        // Sum of items (5 + 10 = 15) matches amount (15)
        Response<AiMessage> mockResponse = Response.from(AiMessage.from("""
                {
                  "expenses": [
                    {
                      "amount": 15,
                      "category": "Alimente",
                      "location": "Magazin",
                      "person": "Familie",
                      "transactionDate": "2024-03-20",
                      "items": [
                        {"name": "Paine", "price": 5},
                        {"name": "Lapte", "price": 10}
                      ]
                    }
                  ]
                }
                """));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);

        List<ExtractionResponse> responses = extractionService.process(req);

        assertNotNull(responses);
        ExtractionResponse response = responses.get(0);
        assertTrue(response.getValidationNote().contains("REUȘITĂ"));
        assertTrue(response.getValidationNote().contains("2 articole"));
        verifyNoInteractions(syncService);
    }

    @Test
    void testConsistencyValidationWarning() {
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText("Paine 5 lei, Lapte 10 lei. Total 20 lei.");

        // Sum of items (5 + 10 = 15) does NOT match amount (20)
        Response<AiMessage> mockResponse = Response.from(AiMessage.from("""
                {
                  "expenses": [
                    {
                      "amount": 20,
                      "category": "Alimente",
                      "location": "Magazin",
                      "person": "Familie",
                      "transactionDate": "2024-03-20",
                      "items": [
                        {"name": "Paine", "price": 5},
                        {"name": "Lapte", "price": 10}
                      ]
                    }
                  ]
                }
                """));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);

        List<ExtractionResponse> responses = extractionService.process(req);

        assertNotNull(responses);
        ExtractionResponse response = responses.get(0);
        assertTrue(response.getValidationNote().contains("AVERTISMENT"));
        assertTrue(response.getValidationNote().contains("15")); // sum
        assertTrue(response.getValidationNote().contains("20")); // total
        verifyNoInteractions(syncService);
    }

    @Test
    void testMultipleExpensesDiarization() {
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText("Eu am luat paine de 5 lei si Maria a luat flori de 50 lei");

        Response<AiMessage> mockResponse = Response.from(AiMessage.from("""
                {
                  "expenses": [
                    {
                      "amount": 5,
                      "category": "Alimente",
                      "location": "Magazin",
                      "person": "Eu",
                      "transactionDate": "2024-03-20"
                    },
                    {
                      "amount": 50,
                      "category": "Cadouri",
                      "location": "Florarie",
                      "person": "Maria",
                      "transactionDate": "2024-03-20"
                    }
                  ]
                }
                """));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);

        List<ExtractionResponse> responses = extractionService.process(req);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("Eu", responses.get(0).getPerson());
        assertEquals("Maria", responses.get(1).getPerson());
        verifyNoInteractions(syncService);
    }

    @Test
    void testUsesLlmExtractedDate() {
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText("Am platit 100 lei la Mega Image");

        Response<AiMessage> mockResponse = Response.from(AiMessage.from("""
                {"expenses": [{"amount": 100, "category": "mancare", "location": "Mega Image", "person": "Familie", "transactionDate": "2024-03-15"}]}
                """));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);

        List<ExtractionResponse> responses = extractionService.process(req);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(LocalDate.of(2024, 3, 15), responses.get(0).getTransactionDate());
        verifyNoInteractions(syncService);
    }

    @Test
    void testFallsBackToNormalizerWhenLlmDateMissing() {
        ExtractionRequest req = new ExtractionRequest();
        req.setRawText("Am platit 100 lei la Mega Image azi");

        Response<AiMessage> mockResponse = Response.from(AiMessage.from("""
                {"expenses": [{"amount": 100, "category": "mancare", "location": "Mega Image", "person": "Familie"}]}
                """));
        when(chatLanguageModel.generate(anyList())).thenReturn(mockResponse);

        List<ExtractionResponse> responses = extractionService.process(req);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        // When LLM date is missing, NormalizerUtil.normalizeDate is called.
        // "azi" should resolve to today.
        assertEquals(LocalDate.now(), responses.get(0).getTransactionDate());
        verifyNoInteractions(syncService);
    }
}
