package com.familie.cheltuieli_familie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReceiptParserTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @InjectMocks
    private ReceiptParser receiptParser;

    @Test
    void testParseReceiptNullText() {
        assertNull(receiptParser.parseReceipt(null));
    }

    @Test
    void testParseReceiptBlankText() {
        assertNull(receiptParser.parseReceipt("   "));
    }

    @Test
    void testParseReceiptEmptyText() {
        assertNull(receiptParser.parseReceipt(""));
    }

    @Test
    void testNormalizeDateValidFormats() throws Exception {
        Method method = ReceiptParser.class.getDeclaredMethod("normalizeDate", String.class);
        method.setAccessible(true);

        assertEquals("2024-03-15", method.invoke(receiptParser, "15/03/2024"));
        assertEquals("2024-04-20", method.invoke(receiptParser, "20.04.2024"));
        assertEquals("2024-01-01", method.invoke(receiptParser, "2024-01-01"));
        assertEquals("2024-12-25", method.invoke(receiptParser, "25-12-2024"));
    }

    @Test
    void testNormalizeDateInvalidFormat() throws Exception {
        Method method = ReceiptParser.class.getDeclaredMethod("normalizeDate", String.class);
        method.setAccessible(true);

        assertNull(method.invoke(receiptParser, "not-a-date"));
    }

    @Test
    void testNormalizeDateNull() throws Exception {
        Method method = ReceiptParser.class.getDeclaredMethod("normalizeDate", String.class);
        method.setAccessible(true);

        assertNull(method.invoke(receiptParser, (String) null));
        assertNull(method.invoke(receiptParser, "   "));
    }

    @Test
    void testNormalizeText() throws Exception {
        Method method = ReceiptParser.class.getDeclaredMethod("normalizeText", String.class);
        method.setAccessible(true);

        assertEquals("Kaufland", method.invoke(receiptParser, "  Kaufland  "));
        assertEquals("Test", method.invoke(receiptParser, "Test"));
        assertNull(method.invoke(receiptParser, (String) null));
    }

    @Test
    void testStripMarkdownFences() throws Exception {
        Method method = ReceiptParser.class.getDeclaredMethod("stripMarkdownFences", String.class);
        method.setAccessible(true);

        assertEquals("{\"store\":\"Lidl\"}\n", method.invoke(receiptParser, "```json\n{\"store\":\"Lidl\"}\n```"));
        assertEquals("{\"store\":\"Lidl\"}", method.invoke(receiptParser, "```\n{\"store\":\"Lidl\"}\n```"));
        assertEquals("no fences", method.invoke(receiptParser, "no fences"));
        assertNull(method.invoke(receiptParser, (String) null));
    }

    @Test
    void testParsedReceiptFields() {
        ReceiptParser.ParsedReceipt receipt = new ReceiptParser.ParsedReceipt();
        receipt.setStoreName("Test Store");
        receipt.setTotalAmount(new BigDecimal("100.00"));
        receipt.setDate("2024-01-01");
        receipt.setCategory("Food");

        ReceiptParser.ReceiptItem item = new ReceiptParser.ReceiptItem();
        item.setName("Milk");
        item.setQuantity(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("5.50"));
        receipt.setItems(java.util.List.of(item));

        assertEquals("Test Store", receipt.getStoreName());
        assertEquals(new BigDecimal("100.00"), receipt.getTotalAmount());
        assertEquals("2024-01-01", receipt.getDate());
        assertEquals("Food", receipt.getCategory());
        assertNotNull(receipt.getItems());
        assertEquals(1, receipt.getItems().size());
        assertEquals("Milk", receipt.getItems().get(0).getName());
        assertEquals(new BigDecimal("2"), receipt.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("5.50"), receipt.getItems().get(0).getUnitPrice());
    }

    @Test
    void testParsedReceiptNullItems() {
        ReceiptParser.ParsedReceipt receipt = new ReceiptParser.ParsedReceipt();
        receipt.setStoreName("Store");
        receipt.setTotalAmount(new BigDecimal("50.00"));
        receipt.setItems(null);

        assertNull(receipt.getItems());
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"storeName\":\"Lidl\",\"totalAmount\":45.30,\"date\":\"15/03/2024\",\"category\":\"Mancare\",\"items\":[{\"name\":\"Lapte\",\"quantity\":2,\"unitPrice\":5.50}]}";
        ObjectMapper mapper = new ObjectMapper();
        ReceiptParser.ParsedReceipt receipt = mapper.readValue(json, ReceiptParser.ParsedReceipt.class);

        assertEquals("Lidl", receipt.getStoreName());
        assertEquals(new BigDecimal("45.30"), receipt.getTotalAmount());
        assertEquals("15/03/2024", receipt.getDate());
        assertEquals("Mancare", receipt.getCategory());
        assertNotNull(receipt.getItems());
        assertEquals(1, receipt.getItems().size());
        assertEquals("Lapte", receipt.getItems().get(0).getName());
    }
}
