package com.proiect.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ExtractionResponse {
    private BigDecimal amount;
    private String category;
    private String location;
    private String person;
    private LocalDate transactionDate;
    private String rawInput;
    private String validationNote;
}
