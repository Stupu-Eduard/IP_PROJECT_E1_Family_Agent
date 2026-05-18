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
}

