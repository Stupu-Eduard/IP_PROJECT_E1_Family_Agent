package com.proiect.m3.extraction.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    private String category;
    private String location;
    private String person;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "raw_input", length = 1000)
    private String rawInput;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
