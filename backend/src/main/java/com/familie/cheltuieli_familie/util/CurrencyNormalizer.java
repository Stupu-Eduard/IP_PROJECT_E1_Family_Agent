package com.familie.cheltuieli_familie.util;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class CurrencyNormalizer {

    private CurrencyNormalizer() {}

    private static final Map<String, BigDecimal> ROMANIAN_NUMBERS = new LinkedHashMap<>();

    static {
        ROMANIAN_NUMBERS.put("o sută jumate", new BigDecimal("150.0"));
        ROMANIAN_NUMBERS.put("două sute", new BigDecimal("200.0"));
        ROMANIAN_NUMBERS.put("o mie", new BigDecimal("1000.0"));
        ROMANIAN_NUMBERS.put("două mii", new BigDecimal("2000.0"));
        ROMANIAN_NUMBERS.put("un milion", new BigDecimal("1000000.0"));
        ROMANIAN_NUMBERS.put("o milioană", new BigDecimal("1000000.0"));
        ROMANIAN_NUMBERS.put("două milioane", new BigDecimal("2000000.0"));
    }

    public static BigDecimal parseRomanianAmount(String text) {
        if (text == null) {
            return null;
        }

        String cleanText = text.toLowerCase().trim();

        for (Map.Entry<String, BigDecimal> entry : ROMANIAN_NUMBERS.entrySet()) {
            if (cleanText.contains(entry.getKey())) {
                return entry.getValue();
            }
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
        return "RON";
    }
}
