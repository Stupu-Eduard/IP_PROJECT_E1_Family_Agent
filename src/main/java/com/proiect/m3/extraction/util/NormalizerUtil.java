package com.proiect.m3.extraction.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NormalizerUtil {

    private static final Map<String, Integer> MONTHS = new HashMap<>();
    static {
        MONTHS.put("ianuarie", 1); MONTHS.put("februarie", 2); MONTHS.put("martie", 3);
        MONTHS.put("aprilie", 4); MONTHS.put("mai", 5); MONTHS.put("iunie", 6);
        MONTHS.put("iulie", 7); MONTHS.put("august", 8); MONTHS.put("septembrie", 9);
        MONTHS.put("octombrie", 10); MONTHS.put("noiembrie", 11); MONTHS.put("decembrie", 12);
    }

    public static BigDecimal normalizeAmount(String text) {
        if (text == null) return null;
        
        String cleanText = text.toLowerCase().trim();
        
        if (cleanText.contains("o sută jumate")) return new BigDecimal("150.0");
        if (cleanText.contains("două sute")) return new BigDecimal("200.0");
        if (cleanText.contains("o mie")) return new BigDecimal("1000.0");
        
        try {
            // Remove non-numeric except dot and comma
            String numericPart = cleanText.replaceAll("[^0-9.,]", "");
            if (numericPart.isEmpty()) return null;

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

    public static LocalDateTime normalizeDate(String text) {
        if (text == null) return LocalDateTime.now();
        
        String cleanText = text.toLowerCase().trim();
        LocalDate date = LocalDate.now();
        
        if (cleanText.contains("alaltăieri")) {
            date = date.minusDays(2);
        } else if (cleanText.contains("ieri")) {
            date = date.minusDays(1);
        } else if (cleanText.contains("astăzi") || cleanText.contains("azi")) {
            date = LocalDate.now();
        } else {
            // Try to match patterns like "15 ianuarie"
            for (Map.Entry<String, Integer> entry : MONTHS.entrySet()) {
                if (cleanText.contains(entry.getKey())) {
                    Pattern pattern = Pattern.compile("(\\d{1,2})\\s+" + entry.getKey());
                    Matcher matcher = pattern.matcher(cleanText);
                    if (matcher.find()) {
                        int day = Integer.parseInt(matcher.group(1));
                        int month = entry.getValue();
                        int year = LocalDate.now().getYear();
                        return LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.now());
                    }
                }
            }
            
            // Try standard formats like 20.10.2023 or 20-10-2023
            Pattern datePattern = Pattern.compile("(\\d{1,2})[./-](\\d{1,2})[./-](\\d{2,4})");
            Matcher matcher = datePattern.matcher(cleanText);
            if (matcher.find()) {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                if (year < 100) year += 2000;
                try {
                    return LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.now());
                } catch (Exception e) {
                    // Ignore invalid dates
                }
            }
        }
        
        return LocalDateTime.of(date, LocalTime.now());
    }
}
