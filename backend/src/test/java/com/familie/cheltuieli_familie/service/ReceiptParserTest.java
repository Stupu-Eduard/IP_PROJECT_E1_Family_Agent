package com.familie.cheltuieli_familie.service;

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
        receipt.storeName = "Test Store";
        receipt.totalAmount = new BigDecimal("100.00");
        receipt.date = "2024-01-01";
        receipt.category = "Food";

        ReceiptParser.ReceiptItem item = new ReceiptParser.ReceiptItem();
        item.name = "Milk";
        item.quantity = new BigDecimal("2");
        item.unitPrice = new BigDecimal("5.50");
        receipt.items = java.util.List.of(item);

        assertEquals("Test Store", receipt.storeName);
        assertEquals(new BigDecimal("100.00"), receipt.totalAmount);
        assertEquals("2024-01-01", receipt.date);
        assertEquals("Food", receipt.category);
        assertNotNull(receipt.items);
        assertEquals(1, receipt.items.size());
        assertEquals("Milk", receipt.items.get(0).name);
        assertEquals(new BigDecimal("2"), receipt.items.get(0).quantity);
        assertEquals(new BigDecimal("5.50"), receipt.items.get(0).unitPrice);
    }

    @Test
    void testParsedReceiptNullItems() {
        ReceiptParser.ParsedReceipt receipt = new ReceiptParser.ParsedReceipt();
        receipt.storeName = "Store";
        receipt.totalAmount = new BigDecimal("50.00");
        receipt.items = null;

        assertNull(receipt.items);
    }
}
