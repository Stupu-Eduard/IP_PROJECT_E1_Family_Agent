package com.familie.cheltuieli_familie.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class NormalizerUtilTest {

    @Test
    void testNormalizeAmountWithRon() {
        BigDecimal result = NormalizerUtil.normalizeAmount("100 RON");
        assertEquals(new BigDecimal("100"), result);
    }

    @Test
    void testNormalizeAmountWithLei() {
        BigDecimal result = NormalizerUtil.normalizeAmount("50.5 lei");
        assertEquals(new BigDecimal("50.5"), result);
    }

    @Test
    void testNormalizeDateToday() {
        LocalDate result = NormalizerUtil.normalizeDate("azi");
        assertEquals(LocalDate.now(), result);
    }

    @Test
    void testNormalizeDateYesterday() {
        LocalDate result = NormalizerUtil.normalizeDate("ieri");
        assertEquals(LocalDate.now().minusDays(1), result);
    }
}
