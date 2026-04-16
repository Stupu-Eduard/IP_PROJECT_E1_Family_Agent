package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_items")
@Data
public class ExpenseItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;
    private String description;

    //relatie inversa catre expense (parinte) (din UML: "contains")
    @ManyToOne
    @JoinColumn(name = "expense_id")
    private Expense expense;

}
