package com.proiect.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CurrencyNormalizerTest {

    @Test
    void testParseRomanianAmount_standardDecimal() {
        assertEquals(new BigDecimal("50.5"), CurrencyNormalizer.parseRomanianAmount("50.5"));
        assertEquals(new BigDecimal("123.45"), CurrencyNormalizer.parseRomanianAmount("123.45"));
    }

    @Test
    void testParseRomanianAmount_withComma() {
        assertEquals(new BigDecimal("150.0"), CurrencyNormalizer.parseRomanianAmount("150,0"));
        assertEquals(new BigDecimal("1234.56"), CurrencyNormalizer.parseRomanianAmount("1.234,56"));
    }

    @Test
    void testParseRomanianAmount_romanianExpressions() {
        assertEquals(new BigDecimal("150.0"), CurrencyNormalizer.parseRomanianAmount("o sută jumate"));
        assertEquals(new BigDecimal("200.0"), CurrencyNormalizer.parseRomanianAmount("două sute"));
        assertEquals(new BigDecimal("1000.0"), CurrencyNormalizer.parseRomanianAmount("o mie"));
        assertEquals(new BigDecimal("2000.0"), CurrencyNormalizer.parseRomanianAmount("două mii"));
        assertEquals(new BigDecimal("1000000.0"), CurrencyNormalizer.parseRomanianAmount("un milion"));
        assertEquals(new BigDecimal("2000000.0"), CurrencyNormalizer.parseRomanianAmount("două milioane"));
    }

    @Test
    void testParseRomanianAmount_withCurrencySymbol() {
        assertEquals(0, new BigDecimal("89.0").compareTo(CurrencyNormalizer.parseRomanianAmount("89 lei")));
        assertEquals(0, new BigDecimal("120.0").compareTo(CurrencyNormalizer.parseRomanianAmount("120 RON")));
    }

    @Test
    void testParseRomanianAmount_nullAndEmpty() {
        assertNull(CurrencyNormalizer.parseRomanianAmount(null));
        assertNull(CurrencyNormalizer.parseRomanianAmount(""));
        assertNull(CurrencyNormalizer.parseRomanianAmount("   "));
    }

    @Test
    void testDetectCurrency() {
        assertEquals("RON", CurrencyNormalizer.detectCurrency("Am platit 50 lei"));
        assertEquals("RON", CurrencyNormalizer.detectCurrency("100 RON"));
        assertEquals("EUR", CurrencyNormalizer.detectCurrency("25 euro"));
        assertEquals("EUR", CurrencyNormalizer.detectCurrency("30 EUR"));
        assertEquals("EUR", CurrencyNormalizer.detectCurrency("15€"));
        assertEquals("USD", CurrencyNormalizer.detectCurrency("20 usd"));
        assertEquals("USD", CurrencyNormalizer.detectCurrency("10$"));
        assertEquals("RON", CurrencyNormalizer.detectCurrency("random text without currency"));
        assertEquals("RON", CurrencyNormalizer.detectCurrency(null));
    }
}
