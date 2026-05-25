package com.familie.cheltuieli_familie.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private ReceiptParser receiptParser;

    @Test
    void parseReceipt_shouldReturnNullForBlankText() {
        assertNull(receiptParser.parseReceipt(""));
        assertNull(receiptParser.parseReceipt("   "));
        assertNull(receiptParser.parseReceipt(null));
    }

    @Test
    void parseReceipt_shouldParseValidReceipt() {
        String json = """
            {
              "storeName": "Lidl",
              "totalAmount": 150.50,
              "date": "15/03/2025",
              "category": "Mâncare",
              "items": [
                {"name": "Milk", "quantity": 2, "unitPrice": 5.50}
              ]
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("Lidl receipt text");

        assertNotNull(result);
        assertEquals("Lidl", result.getStoreName());
        assertEquals(new BigDecimal("150.50"), result.getTotalAmount());
        assertEquals("2025-03-15", result.getDate());
        assertEquals("Mâncare", result.getCategory());
    }

    @ParameterizedTest
    @CsvSource({
        "l1dl, Lidl",
        "kaufl@nd, Kaufland",
        "'mega 1mage', Mega Image",
        "peny, Penny"
    })
    void parseReceipt_shouldNormalizeStoreName(String rawName, String expectedName) {
        String json = """
            {
              "storeName": "%s",
              "totalAmount": 100.00,
              "date": "01/01/2025",
              "category": "Food"
            }
            """.formatted(rawName);

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("some receipt");

        assertNotNull(result);
        assertEquals(expectedName, result.getStoreName());
    }

    @Test
    void parseReceipt_shouldReturnNullForInvalidAmount() {
        String json = """
            {
              "storeName": "Test",
              "totalAmount": -10.00,
              "date": "01/01/2025",
              "category": "Food"
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("invalid receipt");

        assertNull(result);
    }

    @Test
    void parseReceipt_shouldReturnNullForZeroAmount() {
        String json = """
            {
              "storeName": "Test",
              "totalAmount": 0,
              "date": "01/01/2025",
              "category": "Food"
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("zero amount receipt");

        assertNull(result);
    }

    @Test
    void parseReceipt_shouldHandleMarkdownFences() {
        String json = """
            ```json
            {
              "storeName": "Kaufland",
              "totalAmount": 250.00,
              "date": "10/03/2025",
              "category": "Food"
            }
            ```
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("markdown receipt");

        assertNotNull(result);
        assertEquals("Kaufland", result.getStoreName());
    }

    @Test
    void parseReceipt_shouldHandleInvalidJson() {
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("not json")));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("bad receipt");

        assertNull(result);
    }

    @Test
    void parseReceipt_shouldHandleNullDate() {
        String json = """
            {
              "storeName": "Test",
              "totalAmount": 100.00,
              "date": null,
              "category": "Food"
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("null date receipt");

        assertNotNull(result);
        assertNull(result.getDate());
    }

    @Test
    void parseReceipt_shouldHandleDifferentDateFormats() {
        String json = """
            {
              "storeName": "Test",
              "totalAmount": 100.00,
              "date": "2025-03-15",
              "category": "Food"
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("iso date receipt");

        assertNotNull(result);
        assertEquals("2025-03-15", result.getDate());
    }


    @Test
    void parseReceipt_shouldHandleEmptyItems() {
        String json = """
            {
              "storeName": "Test",
              "totalAmount": 50.00,
              "date": "01/01/2025",
              "category": "Food",
              "items": []
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("empty items");

        assertNotNull(result);
        assertNotNull(result.getItems());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void parseReceipt_shouldSanitizeControlCharacters() {
        String json = """
            {
              "storeName": "Test",
              "totalAmount": 100.00,
              "date": "01/01/2025",
              "category": "Food"
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        String ocrWithControlChars = "Receipt text with \u0000\u0001 control chars";
        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt(ocrWithControlChars);

        assertNotNull(result);
    }

    @Test
    void parseReceipt_shouldTruncateLongText() {
        String json = """
            {
              "storeName": "Test",
              "totalAmount": 100.00,
              "date": "01/01/2025",
              "category": "Food"
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 9000; i++) {
            longText.append("a");
        }

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt(longText.toString());

        assertNotNull(result);
    }
}