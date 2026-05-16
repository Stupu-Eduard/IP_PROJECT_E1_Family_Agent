package com.familie.cheltuieli_familie.dto;

import java.math.BigDecimal;

public record OcrResponseDTO(
    BigDecimal amount,
    String category,
    String date,
    String locationName,
    double confidence
) {}
