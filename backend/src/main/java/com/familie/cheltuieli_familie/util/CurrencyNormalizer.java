package com.familie.cheltuieli_familie.util;

import java.math.BigDecimal;

public class CurrencyNormalizer {

    public static BigDecimal parseRomanianAmount(String text) {
        if (text == null) {
            return null;
        }

        String cleanText = text.toLowerCase().trim();
        BigDecimal specialValue = parseSpecialRomanianCases(cleanText);
        if (specialValue != null) {
            return specialValue;
        }

        return parseNumericPart(cleanText);
    }

    private static BigDecimal parseSpecialRomanianCases(String cleanText) {
        if (cleanText.contains("o sută jumate")) {
            return new BigDecimal("150.0");
        }
        if (cleanText.contains("două sute")) {
            return new BigDecimal("200.0");
        }
        if (cleanText.contains("o mie")) {
            return new BigDecimal("1000.0");
        }
        if (cleanText.contains("două mii")) {
            return new BigDecimal("2000.0");
        }
        if (cleanText.contains("un milion") || cleanText.contains("o milioană")) {
            return new BigDecimal("1000000.0");
        }
        if (cleanText.contains("două milioane")) {
            return new BigDecimal("2000000.0");
        }
        return null;
    }

    private static BigDecimal parseNumericPart(String cleanText) {
        try {
            String numericPart = cleanText.replaceAll("[^0-9.,]", "");
            if (numericPart.isEmpty()) {
                return null;
            }
            // Remove multiple dots or commas at the end which might come from JSON/text context
            numericPart = numericPart.replaceAll("[,.]+$", "");
            if (numericPart.isEmpty()) {
                return null;
            }
            return new BigDecimal(normalizeDecimalSeparator(numericPart));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeDecimalSeparator(String numericPart) {
        if (numericPart.contains(",") && numericPart.contains(".")) {
            if (numericPart.lastIndexOf(",") > numericPart.lastIndexOf(".")) {
                return numericPart.replace(".", "").replace(",", ".");
            }
            return numericPart.replace(",", "");
        }
        return numericPart.replace(",", ".");
    }

    public static String detectCurrency(String text) {
        if (text == null) {
            return "RON";
        }
        String lower = text.toLowerCase();
        if (lower.contains("€") || lower.contains("eur") || lower.contains("euro")) {
            return "EUR";
        }
        if (lower.contains("$") || lower.contains("usd") || lower.contains("dolar")) {
            return "USD";
        }
        if (lower.contains("lei") || lower.contains("ron")) {
            return "RON";
        }
        return "RON";
    }
}
