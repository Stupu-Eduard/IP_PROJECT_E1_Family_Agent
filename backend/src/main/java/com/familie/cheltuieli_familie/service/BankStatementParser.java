package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class BankStatementParser {

    private static final Logger logger =
            LoggerFactory.getLogger(BankStatementParser.class);

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String DEFAULT_CURRENCY = "RON";
    private static final String DEFAULT_TYPE = "EXPENSE";
    private static final String TYPE_INCOME = "INCOME";
    private static final String TYPE_TRANSFER = "TRANSFER";
    private static final String KEYWORD_TRANSFER = TYPE_TRANSFER;

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
            parseLine(line).ifPresent(transaction ->
                    addIfNotDuplicate(transactions, uniqueTransactions, transaction)
            );
        }

        logger.info("FINAL PARSARE | Total: {}", transactions.size());
        return transactions;
    }

    private Optional<Transaction> parseLine(String originalLine) {
        try {
            String line = cleanLine(originalLine);

            if (line.isBlank()) {
                return Optional.empty();
            }

            ParsedDate parsedDate = extractDate(line);
            ParsedAmount parsedAmount = extractAmountAndCurrency(line);

            if (!hasValidDateAndAmount(parsedDate, parsedAmount)) {
                return Optional.empty();
            }

            String description = extractDescription(
                    line,
                    parsedDate.endIndex(),
                    parsedAmount.amountStartIndex()
            );

            description = normalizeDiacritics(description);

            if (!isValidDescription(description)) {
                return Optional.empty();
            }

            String currency = resolveCurrency(parsedAmount.currency(), line);
            String type = extractType(line);

            return Optional.of(new Transaction(
                    parsedDate.date(),
                    description,
                    parsedAmount.amount(),
                    currency,
                    type
            ));

        } catch (Exception e) {
            logger.warn("Linie ignorata: {} | {}", originalLine, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean hasValidDateAndAmount(ParsedDate parsedDate, ParsedAmount parsedAmount) {
        return parsedDate != null && parsedAmount != null && parsedAmount.amount() > 0;
    }

    private void addIfNotDuplicate(
            List<Transaction> transactions,
            Set<String> uniqueTransactions,
            Transaction transaction
    ) {
        String key = buildTransactionKey(
                transaction.getDate(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getType()
        );

        if (uniqueTransactions.add(key)) {
            transactions.add(transaction);
        } else {
            logger.warn("DUPLICAT IGNORAT: {}", key);
        }
    }

    private String cleanLine(String line) {
        if (line == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean previousWasSpace = false;

        for (char character : line.trim().toCharArray()) {
            previousWasSpace = appendNormalizedCharacter(result, character, previousWasSpace);
        }

        return result.toString().trim();
    }

    private boolean appendNormalizedCharacter(
            StringBuilder result,
            char character,
            boolean previousWasSpace
    ) {
        if (Character.isWhitespace(character)) {
            return appendSingleSpace(result, previousWasSpace);
        }

        result.append(character);
        return false;
    }

    private boolean appendSingleSpace(StringBuilder result, boolean previousWasSpace) {
        if (!previousWasSpace) {
            result.append(' ');
        }

        return true;
    }

    private ParsedDate extractDate(String line) {
        String[] tokens = line.split(" ");

        if (tokens.length == 0) {
            return null;
        }

        String firstToken = tokens[0];
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

        int amountTokenIndex = findAmountTokenIndex(tokens);

        if (amountTokenIndex < 0) {
            return null;
        }

        String currency = findCurrency(tokens);
        String amountToken = tokens[amountTokenIndex].replace("O", "0");
        double amount = parseAmount(amountToken);

        if (amount <= 0) {
            return null;
        }

        int amountStartIndex = line.lastIndexOf(tokens[amountTokenIndex]);

        return new ParsedAmount(amount, currency, amountStartIndex);
    }

    private int findAmountTokenIndex(String[] tokens) {
        int lastIndex = tokens.length - 1;
        String possibleCurrency = normalizeCurrency(tokens[lastIndex]);

        if (possibleCurrency != null) {
            return lastIndex - 1;
        }

        return lastIndex;
    }

    private String findCurrency(String[] tokens) {
        String lastToken = tokens[tokens.length - 1];
        return normalizeCurrency(lastToken);
    }

    private double parseAmount(String amountStr) {
        if (!isValidAmountToken(amountStr)) {
            return -1;
        }

        try {
            return Double.parseDouble(amountStr.replace(",", "."));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean isValidAmountToken(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        AmountTokenState state = analyzeAmountToken(value);
        return state.hasDigits()
                && (!state.hasSeparator()
                || isValidDecimalPart(state.digitsAfterSeparator()));
    }

    private AmountTokenState analyzeAmountToken(String value) {
        int separatorCount = 0;
        int digitsAfterSeparator = 0;
        boolean separatorFound = false;
        boolean hasDigits = false;

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            if (isDecimalSeparator(character)) {
                separatorCount++;
                separatorFound = true;

                if (separatorCount > 1) {
                    return new AmountTokenState(false, true, digitsAfterSeparator);
                }
            } else if (Character.isDigit(character)) {
                hasDigits = true;

                if (separatorFound) {
                    digitsAfterSeparator++;
                }
            } else {
                return new AmountTokenState(false, separatorFound, digitsAfterSeparator);
            }
        }

        return new AmountTokenState(hasDigits, separatorFound, digitsAfterSeparator);
    }

    private boolean isDecimalSeparator(char character) {
        return character == '.' || character == ',';
    }

    private boolean isValidDecimalPart(int digitsAfterSeparator) {
        return digitsAfterSeparator >= 1 && digitsAfterSeparator <= 2;
    }

    private String normalizeCurrency(String currencyStr) {
        if (currencyStr == null || currencyStr.isBlank()) {
            return null;
        }

        String currency = currencyStr.toUpperCase(Locale.ROOT).replace("0", "O");

        if (DEFAULT_CURRENCY.equals(currency)) {
            return DEFAULT_CURRENCY;
        }

        if ("EUR".equals(currency)) {
            return "EUR";
        }

        if ("USD".equals(currency)) {
            return "USD";
        }

        if ("GBP".equals(currency)) {
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

    private String resolveCurrency(String parsedCurrency, String line) {
        if (parsedCurrency != null) {
            return parsedCurrency;
        }

        return extractCurrencyFromLine(line);
    }

    private String extractCurrencyFromLine(String line) {
        String upperLine = line.toUpperCase(Locale.ROOT);

        if (upperLine.contains(DEFAULT_CURRENCY) || upperLine.contains("R0N")) {
            return DEFAULT_CURRENCY;
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
        String upperLine = line.toUpperCase(Locale.ROOT);

        if (containsIncomeKeyword(upperLine)) {
            return TYPE_INCOME;
        }

        if (containsTransferKeyword(upperLine)) {
            return TYPE_TRANSFER;
        }

        return DEFAULT_TYPE;
    }

    private boolean containsIncomeKeyword(String upperLine) {
        return upperLine.contains("INCASARE")
                || upperLine.contains("INTRARE")
                || upperLine.contains("CREDIT")
                || upperLine.contains("SALARIU")
                || upperLine.contains("DEPUNERE");
    }

    private boolean containsTransferKeyword(String upperLine) {
        return upperLine.contains(KEYWORD_TRANSFER)
                || upperLine.contains("VIRAMENT");
    }

    private boolean isValidDescription(String description) {
        return description != null
                && !description.isBlank()
                && hasValidDescriptionLength(description)
                && !containsOnlyNumbersAndSeparators(description);
    }

    private boolean hasValidDescriptionLength(String description) {
        return description.length() >= MIN_DESCRIPTION_LENGTH
                && description.length() <= MAX_DESCRIPTION_LENGTH;
    }

    private boolean containsOnlyNumbersAndSeparators(String description) {
        for (char character : description.toCharArray()) {
            if (!isNumberOrSeparator(character)) {
                return false;
            }
        }

        return true;
    }

    private boolean isNumberOrSeparator(char character) {
        return Character.isDigit(character)
                || Character.isWhitespace(character)
                || character == '.'
                || character == ','
                || character == '-';
    }

    private String normalizeDiacritics(String text) {
        if (text == null) {
            return null;
        }

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
        return date + "|"
                + description.toLowerCase(Locale.ROOT) + "|"
                + amount + "|"
                + currency.toUpperCase(Locale.ROOT) + "|"
                + type.toUpperCase(Locale.ROOT);
    }

    private record ParsedDate(LocalDate date, int endIndex) {
    }

    private record ParsedAmount(double amount, String currency, int amountStartIndex) {
    }

    private record AmountTokenState(
            boolean hasDigits,
            boolean hasSeparator,
            int digitsAfterSeparator
    ) {
    }
}