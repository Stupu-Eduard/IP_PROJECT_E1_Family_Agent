package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class BankStatementParser {

    private static final Logger logger =
            LoggerFactory.getLogger(BankStatementParser.class);

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
            processSingleLine(line, transactions, uniqueTransactions);
        }

        logger.info("FINAL PARSARE | Total: {}", transactions.size());
        return transactions;
    }

    private void processSingleLine(String line, List<Transaction> transactions, Set<String> uniqueTransactions) {
        try {
            line = cleanLine(line);

            if (line.isBlank()) {
                return;
            }

            ParsedDate parsedDate = extractDate(line);
            if (parsedDate == null) {
                return;
            }

            ParsedAmount parsedAmount = extractAmountAndCurrency(line);
            if (parsedAmount == null || parsedAmount.amount <= 0) {
                return;
            }

            String description = extractDescription(
                    line,
                    parsedDate.endIndex,
                    parsedAmount.amountStartIndex
            );
            description = normalizeDiacritics(description);

            if (!isValidDescription(description)) {
                return;
            }

            String currency = parsedAmount.currency != null
                    ? parsedAmount.currency
                    : extractCurrencyFromLine(line);

            String type = extractType(line);

            String key = buildTransactionKey(
                    parsedDate.date,
                    description,
                    parsedAmount.amount,
                    currency,
                    type
            );

            if (uniqueTransactions.contains(key)) {
                logger.warn("DUPLICAT IGNORAT: {}", key);
                return;
            }

            uniqueTransactions.add(key);

            Transaction transaction = new Transaction(
                    parsedDate.date,
                    description,
                    parsedAmount.amount,
                    currency,
                    type
            );

            transactions.add(transaction);

        } catch (Exception e) {
            logger.warn("Linie ignorata: {} | {}", line, e.getMessage());
        }
    }

    private String cleanLine(String line) {
        if (line == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean previousWasSpace = false;

        for (char character : line.trim().toCharArray()) {
            if (Character.isWhitespace(character)) {
                if (!previousWasSpace) {
                    result.append(' ');
                    previousWasSpace = true;
                }
            } else {
                result.append(character);
                previousWasSpace = false;
            }
        }

        return result.toString().trim();
    }

    private ParsedDate extractDate(String line) {
        if (line.length() < 10) {
            return null;
        }

        String firstToken = line.split(" ")[0];

        LocalDate date = parseDate(firstToken);
        if (date == null) {
            return null;
        }

        return new ParsedDate(date, firstToken.length());
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private ParsedAmount extractAmountAndCurrency(String line) {
        String[] tokens = line.split(" ");

        if (tokens.length < 2) {
            return null;
        }

        int lastIndex = tokens.length - 1;
        String currency = normalizeCurrency(tokens[lastIndex]);

        int amountTokenIndex = currency != null ? lastIndex - 1 : lastIndex;

        if (amountTokenIndex < 0) {
            return null;
        }

        String amountStr = tokens[amountTokenIndex].replace("O", "0");
        double amount = parseAmount(amountStr);

        if (amount <= 0) {
            return null;
        }

        int amountStartIndex = line.lastIndexOf(tokens[amountTokenIndex]);

        return new ParsedAmount(amount, currency, amountStartIndex);
    }

    private double parseAmount(String amountStr) {
        if (!isValidAmountToken(amountStr)) {
            return -1;
        }

        try {
            return Double.parseDouble(amountStr.replace(",", "."));
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean isValidAmountToken(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        int separatorCount = 0;
        int digitsAfterSeparator = 0;
        boolean separatorFound = false;

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            if (character == '.' || character == ',') {
                separatorCount++;
                separatorFound = true;

                if (separatorCount > 1) {
                    return false;
                }

                continue;
            }

            if (!Character.isDigit(character)) {
                return false;
            }

            if (separatorFound) {
                digitsAfterSeparator++;
            }
        }

        return !separatorFound || (digitsAfterSeparator >= 1 && digitsAfterSeparator <= 2);
    }

    private String normalizeCurrency(String currencyStr) {
        if (currencyStr == null || currencyStr.isBlank()) {
            return null;
        }

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

        return null;
    }

    private String extractDescription(String line, int dateEnd, int amountStart) {

        if (amountStart <= dateEnd) {
            return "";
        }

        return cleanLine(line.substring(dateEnd, amountStart));
    }

    private String extractCurrencyFromLine(String line) {
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
            return DEFAULT_TYPE;
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

        return !containsOnlyNumbersAndSeparators(description);
    }

    private boolean containsOnlyNumbersAndSeparators(String description) {
        for (char character : description.toCharArray()) {
            if (!Character.isDigit(character) &&
                    !Character.isWhitespace(character) &&
                    character != '.' &&
                    character != ',' &&
                    character != '-') {
                return false;
            }
        }

        return true;
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

    private static class ParsedDate {
        private final LocalDate date;
        private final int endIndex;

        private ParsedDate(LocalDate date, int endIndex) {
            this.date = date;
            this.endIndex = endIndex;
        }
    }

    private static class ParsedAmount {
        private final double amount;
        private final String currency;
        private final int amountStartIndex;

        private ParsedAmount(double amount, String currency, int amountStartIndex) {
            this.amount = amount;
            this.currency = currency;
            this.amountStartIndex = amountStartIndex;
        }
    }
}