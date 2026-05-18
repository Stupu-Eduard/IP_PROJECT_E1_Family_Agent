package com.familie.cheltuieli_familie.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

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

    @Test
    void testReceiptItemPojo() {
        ReceiptParser.ReceiptItem item = new ReceiptParser.ReceiptItem();
        item.setName("Milk");
        item.setQuantity(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("5.5"));

        assertEquals("Milk", item.getName());
        assertEquals(new BigDecimal("2"), item.getQuantity());
        assertEquals(new BigDecimal("5.5"), item.getUnitPrice());
    }

    @Test
    void parseReceipt_shouldHandleMalformedJson() {
        when(chatLanguageModel.generate(anyList())).thenReturn(Response.from(AiMessage.from("not a json")));
        assertNull(receiptParser.parseReceipt("some text"));
    }

    @Test
    void parsedReceiptPojo() {
        ReceiptParser.ParsedReceipt receipt = new ReceiptParser.ParsedReceipt();
        receipt.setStoreName("Shop");
        receipt.setTotalAmount(new BigDecimal("100"));
        receipt.setDate("2026-05-17");
        receipt.setCategory("Food");
        receipt.setItems(List.of(new ReceiptParser.ReceiptItem()));

        assertEquals("Shop", receipt.getStoreName());
        assertEquals(new BigDecimal("100"), receipt.getTotalAmount());
        assertEquals("2026-05-17", receipt.getDate());
        assertEquals("Food", receipt.getCategory());
        assertEquals(1, receipt.getItems().size());
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
