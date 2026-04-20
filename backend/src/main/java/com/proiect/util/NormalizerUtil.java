package com.proiect.util;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Facade utility that delegates to specialized normalizers.
 * Kept for backward compatibility with existing code.
 */
public class NormalizerUtil {

    public static BigDecimal normalizeAmount(String text) {
        return CurrencyNormalizer.parseRomanianAmount(text);
    }

    public static LocalDate normalizeDate(String text) {
        return DateNormalizer.resolveRelativeDate(text);
    }
}
