package com.proiect.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ExtractionResponse {
    private BigDecimal amount;
    private String category;
    private String location;
    private String person;
    private LocalDateTime transactionDate;
    private String rawInput;
}
