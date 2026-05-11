package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BankStatementParser {

    private static final Logger logger =
            LoggerFactory.getLogger(BankStatementParser.class);

    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("(?<!\\S)(\\d+(?:[.,]\\d{1,2})?)\\s*(R0N|RON|EUR|USD|GBP)?\\s*$", Pattern.CASE_INSENSITIVE);

    private static final Pattern DATE_PATTERN =
            Pattern.compile("\\d{2}/\\d{2}/\\d{4}");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String DEFAULT_CURRENCY = "RON";
    private static final String DEFAULT_TYPE = "EXPENSE";

    private static final int MIN_DESCRIPTION_LENGTH = 2;
    private static final int MAX_DESCRIPTION_LENGTH = 120;

    public List<Transaction> parseText(String ocrText) {

        List<Transaction> transactions = new ArrayList<>();

        Set<String> uniqueTransactions = new HashSet<>();

        if (ocrText == null || ocrText.isBlank()) {
            return transactions;
        }

        String[] lines = ocrText.split("\\r?\\n");

        logger.info("START PARSARE | Linii: {}", lines.length);

        for (String line : lines) {

            try {
                line = cleanLine(line);

                if (line.isBlank()) {
                    continue;
                }

                Matcher dateMatcher = DATE_PATTERN.matcher(line);
                if (!dateMatcher.find()) {
                    continue;
                }

                LocalDate date = parseDate(dateMatcher.group());
                if (date == null) {
                    continue;
                }

                Matcher amountMatcher = AMOUNT_PATTERN.matcher(line);
                if (!amountMatcher.find()) {
                    continue;
                }

                String amountStr = amountMatcher.group(1).replace("O", "0");
                double amount = parseAmount(amountStr);

                if (amount <= 0) {
                    continue;
                }

                String description = extractDescription(line, dateMatcher.end(), amountMatcher.start());
                description = normalizeDiacritics(description);

                if (!isValidDescription(description)) {
                    continue;
                }

                String currency = extractCurrency(amountMatcher.group(2), line);
                String type = extractType(line);

                String key = buildTransactionKey(
                        date,
                        description,
                        amount,
                        currency,
                        type
                );

                if (uniqueTransactions.contains(key)) {
                    logger.warn("DUPLICAT IGNORAT: {}", key);
                    continue;
                }

                uniqueTransactions.add(key);

                Transaction transaction = new Transaction(
                        date,
                        description,
                        amount,
                        currency,
                        type
                );

                transactions.add(transaction);

            } catch (Exception e) {
                logger.warn("Linie ignorata: {} | {}", line, e.getMessage());
            }
        }

        logger.info("FINAL PARSARE | Total: {}", transactions.size());

        return transactions;
    }

    private String cleanLine(String line) {
        if (line == null) return "";
        return line.replaceAll("\\s+", " ").trim();
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private double parseAmount(String amountStr) {
        try {
            return Double.parseDouble(amountStr.replace(",", "."));
        } catch (Exception e) {
            return -1;
        }
    }

    private String extractDescription(String line, int dateEnd, int amountStart) {

        if (amountStart <= dateEnd) {
            return "";
        }

        return line.substring(dateEnd, amountStart)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractCurrency(String currencyStr, String line) {

        if (currencyStr != null && !currencyStr.isBlank()) {
            String currency = currencyStr.toUpperCase().replace("0", "O");

            if (currency.equals("RON")) {
                return "RON";
            }

            if (currency.equals("EUR")) {
                return "EUR";
            }

            if (currency.equals("USD")) {
                return "USD";
            }

            if (currency.equals("GBP")) {
                return "GBP";
            }
        }

        String upperLine = line.toUpperCase();

        if (upperLine.contains("RON") || upperLine.contains("R0N")) {
            return "RON";
        }

        if (upperLine.contains("EUR")) {
            return "EUR";
        }

        if (upperLine.contains("USD")) {
            return "USD";
        }

        if (upperLine.contains("GBP")) {
            return "GBP";
        }

        return DEFAULT_CURRENCY;
    }

    private String extractType(String line) {
        String upperLine = line.toUpperCase();

        if (upperLine.contains("INCASARE") ||
                upperLine.contains("INTRARE") ||
                upperLine.contains("CREDIT") ||
                upperLine.contains("SALARIU") ||
                upperLine.contains("DEPUNERE")) {
            return "INCOME";
        }

        if (upperLine.contains("TRANSFER") ||
                upperLine.contains("VIRAMENT")) {
            return "TRANSFER";
        }

        if (upperLine.contains("PLATA") ||
                upperLine.contains("CARD") ||
                upperLine.contains("POS") ||
                upperLine.contains("RETRAGERE") ||
                upperLine.contains("COMISION")) {
            return "EXPENSE";
        }

        return DEFAULT_TYPE;
    }

    private boolean isValidDescription(String description) {

        if (description == null || description.isBlank()) {
            return false;
        }

        if (description.length() < MIN_DESCRIPTION_LENGTH ||
                description.length() > MAX_DESCRIPTION_LENGTH) {
            return false;
        }

        return !description.matches("^[0-9\\s.,-]+$");
    }

    private String normalizeDiacritics(String text) {

        if (text == null) return null;

        return text
                .replace('Ş', 'Ș')
                .replace('ş', 'ș')
                .replace('Ţ', 'Ț')
                .replace('ţ', 'ț')
                .replace('Ă', 'A')
                .replace('ă', 'a')
                .replace('Â', 'A')
                .replace('â', 'a')
                .replace('Î', 'I')
                .replace('î', 'i');
    }

    private String buildTransactionKey(
            LocalDate date,
            String description,
            double amount,
            String currency,
            String type
    ) {
        return date + "|" +
                description.toLowerCase() + "|" +
                amount + "|" +
                currency.toUpperCase() + "|" +
                type.toUpperCase();
    }
}