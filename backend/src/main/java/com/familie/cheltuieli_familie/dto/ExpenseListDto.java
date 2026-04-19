package com.familie.cheltuieli_familie.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExpenseListDto(
        Long id,
        BigDecimal amount,
        String currency,
        String description,
        LocalDateTime expenseDate,
        String category,
        String person,
        LocationDto location
) {
}
