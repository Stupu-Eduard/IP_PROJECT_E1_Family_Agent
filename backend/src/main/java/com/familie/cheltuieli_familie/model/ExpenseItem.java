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

    @Column(name = "item_name")
    private String itemName; // Task: item_name

    private BigDecimal quantity = BigDecimal.ONE; // Task: quantity (1 default)

    @Column(name = "raw_text")
    private String rawText; // Task: raw_text (pentru OCR)

    //relatie inversa catre expense (parinte) (din UML: "contains")
    @ManyToOne
    @JoinColumn(name = "expense_id")
    private Expense expense;

    //expense_item to category
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

}
