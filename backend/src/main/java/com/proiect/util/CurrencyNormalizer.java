package com.proiect.util;

import java.math.BigDecimal;

public class CurrencyNormalizer {

    public static BigDecimal parseRomanianAmount(String text) {
        if (text == null) {
            return null;
        }

        String cleanText = text.toLowerCase().trim();

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

        try {
            String numericPart = cleanText.replaceAll("[^0-9.,]", "");
            if (numericPart.isEmpty()) {
                return null;
            }

            if (numericPart.contains(",") && numericPart.contains(".")) {
                if (numericPart.lastIndexOf(",") > numericPart.lastIndexOf(".")) {
                    numericPart = numericPart.replace(".", "").replace(",", ".");
                } else {
                    numericPart = numericPart.replace(",", "");
                }
            } else {
                numericPart = numericPart.replace(",", ".");
            }

            return new BigDecimal(numericPart);
        } catch (NumberFormatException e) {
            return null;
        }
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
