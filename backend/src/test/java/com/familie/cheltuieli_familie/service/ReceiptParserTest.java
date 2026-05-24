package com.familie.cheltuieli_familie.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @Test
    void parseReceipt_shouldNormalizeStoreName() {
        String json = """
            {
              "storeName": "l1dl",
              "totalAmount": 100.00,
              "date": "01/01/2025",
              "category": "Food"
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("some receipt");

        assertNotNull(result);
        assertEquals("Lidl", result.getStoreName());
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
    void parseReceipt_shouldNormalizeKauflandOcr() {
        String json = """
            {
              "storeName": "kaufl@nd",
              "totalAmount": 100.00,
              "date": "01/01/2025",
              "category": "Food"
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("ocr receipt");

        assertNotNull(result);
        assertEquals("Kaufland", result.getStoreName());
    }

    @Test
    void parseReceipt_shouldNormalizeMegaImageOcr() {
        String json = """
            {
              "storeName": "mega 1mage",
              "totalAmount": 100.00,
              "date": "01/01/2025",
              "category": "Food"
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(json)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("ocr receipt");

        assertNotNull(result);
        assertEquals("Mega Image", result.getStoreName());
    }

    @Test
    void parseReceipt_shouldNormalizeOcrStoreNames() {
        String mockJsonResponse = """
            {
              "storeName": "L1dl",
              "totalAmount": 45.99,
              "date": "15/05/2026",
              "category": "Mâncare"
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(mockJsonResponse)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("bon l1dl");

        assertNotNull(result);
        assertEquals("Lidl", result.getStoreName());
    }

    @Test
    void parseReceipt_shouldNormalizeKauflardToKaufland() {
        String mockJsonResponse = """
            {
              "storeName": "Kauflard",
              "totalAmount": 123.45,
              "date": "01.05.2026",
              "category": "Mâncare"
            }
            """;

        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(mockJsonResponse)));

        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("bon kauflard");

        assertNotNull(result);
        assertEquals("Kaufland", result.getStoreName());
    }

    @ParameterizedTest
    @CsvSource({
        "L1dl, Lidl",
        "l1d1, Lidl",
        "lid1, Lidl",
        "Kauflard, Kaufland",
        "kauf1and, Kaufland",
        "kaufl@nd, Kaufland",
        "mega 1mage, Mega Image",
        "mega lmage, Mega Image",
        "mega1mage, Mega Image",
        "carref0ur, Carrefour",
        "carrefour, Carrefour",
        "peny, Penny",
        "P3nny, Penny",
        "auch@n, Auchan",
        "auch4n, Auchan",
        "pr0fi, Profi",
        "prof1, Profi",
        "s3lgros, Selgros",
        "se1gros, Selgros",
        "c@tena, Catena",
        "cat3na, Catena",
        "sens1b1u, Sensiblu",
        "sensib1u, Sensiblu",
        "d0na, Dona",
        "p3trom, Petrom",
        "petr0m, Petrom",
        "r0mpetr0l, Rompetrol",
        "0mv, OMV"
    })
    void normalizeStoreName_shouldNormalizeOcrVariants(String input, String expected) {
        String mockJson = String.format("""
            {
              "storeName": "%s",
              "totalAmount": 10.0,
              "date": "01/01/2026",
              "category": "Test"
            }
            """, input);
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(mockJson)));
        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("text");
        assertNotNull(result);
        assertEquals(expected, result.getStoreName());
    }

    @ParameterizedTest
    @CsvSource({
        "2026-05-18, 2026-05-18",
        "18-05-2026, 2026-05-18"
    })
    void parseReceipt_shouldNormalizeVariousDateFormats(String inputDate, String expectedDate) {
        String mockJson = String.format("""
            {
              "storeName": "Test",
              "totalAmount": 10.0,
              "date": "%s",
              "category": "Test"
            }
            """, inputDate);
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(mockJson)));
        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("text");
        assertNotNull(result);
        assertEquals(expectedDate, result.getDate());
    }

    @Test
    void parseReceipt_shouldHandleNullDate() {
        String mockJson = """
            {
              "storeName": "Test",
              "totalAmount": 10.0,
              "date": null,
              "category": "Test"
            }
            """;
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(mockJson)));
        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("text");
        assertNotNull(result);
        assertNull(result.getDate());
    }

    @Test
    void parseReceipt_shouldHandleInvalidDate() {
        String mockJson = """
            {
              "storeName": "Test",
              "totalAmount": 10.0,
              "date": "not-a-date",
              "category": "Test"
            }
            """;
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(mockJson)));
        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("text");
        assertNotNull(result);
        assertNull(result.getDate());
    }

    @Test
    void parseReceipt_shouldHandleNullStoreName() {
        String mockJson = """
            {
              "storeName": null,
              "totalAmount": 10.0,
              "date": "01/01/2026",
              "category": "Test"
            }
            """;
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(mockJson)));
        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("text");
        assertNotNull(result);
        assertNull(result.getStoreName());
    }

    @ParameterizedTest
    @CsvSource({
            "'', ''",
            "Unknown Shop, Unknown Shop",
            "penny market, penny market",
            "mypenyz, mypenyz",
            "xd0nax, xd0nax",
            "xxx0mv, xxx0mv"
    })
    void parseReceipt_shouldNotNormalizeOrPreserveStoreName(String storeName, String expected) {
        String mockJson = String.format("""
            {
              "storeName": "%s",
              "totalAmount": 10.0,
              "date": "01/01/2026",
              "category": "Test"
            }
            """, storeName);
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(mockJson)));
        ReceiptParser.ParsedReceipt result = receiptParser.parseReceipt("text");
        assertNotNull(result);
        assertEquals(expected, result.getStoreName());
    }
}
