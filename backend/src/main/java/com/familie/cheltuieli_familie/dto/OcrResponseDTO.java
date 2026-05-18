package com.familie.cheltuieli_familie.dto;

import java.math.BigDecimal;
import java.util.List;

public record OcrResponseDTO(
    BigDecimal amount,
    String category,
    String date,
    String locationName,
    double confidence,
    List<OcrItemDTO> items
) {}

