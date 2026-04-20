package com.proiect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExtractedExpenseDTO(
    BigDecimal amount,
    String currency,
    String category,
    LocalDate transactionDate,
    String rawText
) {}
