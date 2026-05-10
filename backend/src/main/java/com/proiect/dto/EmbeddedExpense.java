package com.proiect.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class EmbeddedExpense {
    private Long id;
    private BigDecimal amount;
    private String category;
    private String location;
    private String person;
    private LocalDate date;
    private String rawInput;
    private double score; // To hold the similarity score from Qdrant
}
