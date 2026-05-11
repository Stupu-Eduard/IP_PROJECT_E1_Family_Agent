package com.familie.cheltuieli_familie.util;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateNormalizer {

    private static final Map<String, Integer> MONTHS = new HashMap<>();
    static {
        MONTHS.put("ianuarie", 1);
        MONTHS.put("februarie", 2);
        MONTHS.put("martie", 3);
        MONTHS.put("aprilie", 4);
        MONTHS.put("mai", 5);
        MONTHS.put("iunie", 6);
        MONTHS.put("iulie", 7);
        MONTHS.put("august", 8);
        MONTHS.put("septembrie", 9);
        MONTHS.put("octombrie", 10);
        MONTHS.put("noiembrie", 11);
        MONTHS.put("decembrie", 12);
    }

    public static LocalDate resolveRelativeDate(String text) {
        if (text == null) {
            return LocalDate.now();
        }

        String cleanText = text.toLowerCase().trim();
        
        LocalDate relativeDate = resolveRelativeTerms(cleanText);
        if (relativeDate != null) {
            return relativeDate;
        }

        LocalDate monthDate = parseMonthBasedDate(cleanText);
        if (monthDate != null) {
            return monthDate;
        }

        LocalDate numericDate = parseNumericDate(cleanText);
        if (numericDate != null) {
            return numericDate;
        }

        return LocalDate.now();
    }

    private static LocalDate resolveRelativeTerms(String cleanText) {
        if (cleanText.contains("alaltăieri")) {
            return LocalDate.now().minusDays(2);
        }
        if (cleanText.contains("ieri")) {
            return LocalDate.now().minusDays(1);
        }
        if (cleanText.contains("astăzi") || cleanText.contains("azi")) {
            return LocalDate.now();
        }
        if (cleanText.contains("poimâine")) {
            return LocalDate.now().plusDays(2);
        }
        if (cleanText.contains("săptămâna trecută") || cleanText.contains("saptamana trecuta")) {
            return LocalDate.now().minusWeeks(1);
        }
        if (cleanText.contains("luna trecută") || cleanText.contains("luna trecuta")) {
            return LocalDate.now().minusMonths(1);
        }
        return null;
    }

    private static LocalDate parseMonthBasedDate(String cleanText) {
        for (Map.Entry<String, Integer> entry : MONTHS.entrySet()) {
            if (cleanText.contains(entry.getKey())) {
                Pattern pattern = Pattern.compile("(\\d{1,2})\\s+" + entry.getKey());
                Matcher matcher = pattern.matcher(cleanText);
                if (matcher.find()) {
                    int day = Integer.parseInt(matcher.group(1));
                    int month = entry.getValue();
                    int year = LocalDate.now().getYear();
                    return LocalDate.of(year, month, day);
                }
            }
        }
        return null;
    }

    private static LocalDate parseNumericDate(String cleanText) {
        Pattern datePattern = Pattern.compile("(\\d{1,2})[./-](\\d{1,2})[./-](\\d{2,4})");
        Matcher matcher = datePattern.matcher(cleanText);
        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                if (year < 100) {
                    year += 2000;
                }
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                // Ignore invalid dates
            }
        }
        return null;
    }
}
