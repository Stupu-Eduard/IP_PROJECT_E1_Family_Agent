package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BankStatementParser {

    private static final Logger logger = LoggerFactory.getLogger(BankStatementParser.class);

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+[.,]?\\d*)\\s*$");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String DEFAULT_CURRENCY = "RON";
    private static final String DEFAULT_TYPE = "EXPENSE";

    public List<Transaction> parseText(String ocrText) {
        List<Transaction> transactions = new ArrayList<>();

        if (ocrText == null || ocrText.isEmpty()) {
            return transactions;
        }

        String[] lines = ocrText.split("\\r?\\n");
        logger.info("=== START PARSARE OCR. Linii procesate: {} ===", lines.length);

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            line = line.replace("O", "0");

            if (!line.matches(".*\\d{1,2}/\\d{1,2}/\\d{4}.*")) {
                continue;
            }

            try {
                String dateStr = line.substring(0, 10);
                LocalDate date = LocalDate.parse(dateStr, FORMATTER);

                Matcher matcher = AMOUNT_PATTERN.matcher(line);

                if (!matcher.find()) {
                    continue;
                }

                String amountStr = matcher.group(1);
                double amount = Double.parseDouble(amountStr.replace(",", "."));

                if (amount <= 0) {
                    continue;
                }

                String description = line.substring(10, line.lastIndexOf(amountStr)).trim();

                transactions.add(new Transaction(
                        date,
                        description,
                        amount,
                        DEFAULT_CURRENCY,
                        DEFAULT_TYPE
                ));

            } catch (Exception e) {
                logger.warn("Linie ignorata: {} | Eroare: {}", line, e.getMessage());
            }
        }

        logger.info("=== FINAL PARSARE. Total tranzactii gasite: {} ===", transactions.size());
        return transactions;
    }
}