package com.familie.cheltuieli_familie.dto;

import java.math.BigDecimal;

public record OcrItemDTO(
    String name,
    BigDecimal quantity,
    BigDecimal unitPrice
) {}
