package com.proiect.util;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NormalizerUtil {

    public static BigDecimal normalizeAmount(String text) {
        if (text == null) return null;
        
        String cleanText = text.toLowerCase().trim();
        
        if (cleanText.contains("o sută jumate")) {
            return new BigDecimal("150.0");
        }
        
        // Match standard numeric patterns if the AI returns raw text instead of clean numbers
        try {
            String numericPart = cleanText.replaceAll("[^0-9.,]", "").replace(",", ".");
            if (!numericPart.isEmpty()) {
                return new BigDecimal(numericPart);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        
        return null;
    }

    public static LocalDate normalizeDate(String text) {
        if (text == null) return LocalDate.now();
        
        String cleanText = text.toLowerCase().trim();
        LocalDate date = LocalDate.now();
        
        if (cleanText.contains("alaltăieri")) {
            date = date.minusDays(2);
        } else if (cleanText.contains("ieri")) {
            date = date.minusDays(1);
        }
        
        return date;
    }
}
