package com.familie.cheltuieli_familie.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyNormalizerTest {

    @Test
    void testParseRomanianAmountNull() {
        assertNull(CurrencyNormalizer.parseRomanianAmount(null));
    }

    @Test
    void testParseRomanianAmountText() {
        assertEquals(new BigDecimal("150.0"), CurrencyNormalizer.parseRomanianAmount("o sută jumate"));
        assertEquals(new BigDecimal("200.0"), CurrencyNormalizer.parseRomanianAmount("două sute"));
        assertEquals(new BigDecimal("1000.0"), CurrencyNormalizer.parseRomanianAmount("o mie"));
        assertEquals(new BigDecimal("2000.0"), CurrencyNormalizer.parseRomanianAmount("două mii"));
        assertEquals(new BigDecimal("1000000.0"), CurrencyNormalizer.parseRomanianAmount("un milion"));
        assertEquals(new BigDecimal("1000000.0"), CurrencyNormalizer.parseRomanianAmount("o milioană"));
        assertEquals(new BigDecimal("2000000.0"), CurrencyNormalizer.parseRomanianAmount("două milioane"));
    }

    @Test
    void testParseRomanianAmountNumeric() {
        assertEquals(new BigDecimal("123.45"), CurrencyNormalizer.parseRomanianAmount("123.45"));
        assertEquals(new BigDecimal("123.45"), CurrencyNormalizer.parseRomanianAmount("123,45"));
        assertEquals(new BigDecimal("1234.56"), CurrencyNormalizer.parseRomanianAmount("1,234.56"));
        assertEquals(new BigDecimal("1234.56"), CurrencyNormalizer.parseRomanianAmount("1.234,56"));
    }

    @Test
    void testParseRomanianAmountEmpty() {
        assertNull(CurrencyNormalizer.parseRomanianAmount("abc"));
    }

    @Test
    void testDetectCurrency() {
        assertEquals("EUR", CurrencyNormalizer.detectCurrency("100 euro"));
        assertEquals("EUR", CurrencyNormalizer.detectCurrency("100 eur"));
        assertEquals("EUR", CurrencyNormalizer.detectCurrency("100 €"));
        assertEquals("USD", CurrencyNormalizer.detectCurrency("100 usd"));
        assertEquals("USD", CurrencyNormalizer.detectCurrency("100 $"));
        assertEquals("USD", CurrencyNormalizer.detectCurrency("100 dolar"));
        assertEquals("RON", CurrencyNormalizer.detectCurrency("100 lei"));
        assertEquals("RON", CurrencyNormalizer.detectCurrency("100 ron"));
        assertEquals("RON", CurrencyNormalizer.detectCurrency(null));
        assertEquals("RON", CurrencyNormalizer.detectCurrency("random text"));
    }
}
