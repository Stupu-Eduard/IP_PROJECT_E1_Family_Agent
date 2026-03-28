package com.proiect.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expenses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;
    private String currency;
    private String category;
    private LocalDate transactionDate;
    private String rawText;

    public ExpenseEntity(BigDecimal amount, String currency, String category, LocalDate transactionDate, String rawText) {
        this.amount = amount;
        this.currency = currency;
        this.category = category;
        this.transactionDate = transactionDate;
        this.rawText = rawText;
    }
}
