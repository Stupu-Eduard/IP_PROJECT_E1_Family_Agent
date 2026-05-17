package com.familie.cheltuieli_familie.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptParserTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    private ReceiptParser receiptParser;

    @BeforeEach
    void setUp() {
        receiptParser = new ReceiptParser(chatLanguageModel);
    }

    @Test
    void parseReceipt_shouldReturnNull_whenInputEmpty() {
        assertNull(receiptParser.parseReceipt(null));
        assertNull(receiptParser.parseReceipt(""));
    }

    @Test
    void parseReceipt_shouldReturnParsedObject_whenLlmReturnsValidJson() {
        String mockJsonResponse = """
            {
              "storeName": "Mega Image",
              "totalAmount": 150.5,
              "date": "17/05/2026",
              "category": "Mâncare"
            }
            """;
        
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(mockJsonResponse)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("bon fiscal mega image");

        assertNotNull(result);
        assertEquals("Mega Image", result.getStoreName());
        assertEquals(new BigDecimal("150.5"), result.getTotalAmount());
        assertEquals("2026-05-17", result.getDate());
        assertEquals("Mâncare", result.getCategory());
    }

    @Test
    void parseReceipt_shouldHandleMarkdownFences() {
        String mockJsonResponse = """
            ```json
            {
              "storeName": "Lidl",
              "totalAmount": 50.0,
              "date": "10.05.2026",
              "category": "Mâncare"
            }
            ```
            """;
        
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(mockJsonResponse)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("bon lidl");

        assertNotNull(result);
        assertEquals("Lidl", result.getStoreName());
        assertEquals("2026-05-10", result.getDate());
    }

    @Test
    void parseReceipt_shouldReturnNull_whenAmountIsInvalid() {
        String mockJsonResponse = """
            {
              "storeName": "Lidl",
              "totalAmount": 0,
              "date": "10.05.2026"
            }
            """;
        
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(mockJsonResponse)));

        assertNull(receiptParser.parseReceipt("bon lidl"));
    }
}
