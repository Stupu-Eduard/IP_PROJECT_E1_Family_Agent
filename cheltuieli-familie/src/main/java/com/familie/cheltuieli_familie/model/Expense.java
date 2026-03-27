package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "expenses")
@Data
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal sum;
    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}