package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "expenses")
@Data
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount; // Am schimbat din 'sum'
    private String description;

    @Column(name = "date")
    private java.time.LocalDateTime date;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; //expense -> user (Cine a cheltuit?)

    @ManyToOne
    @JoinColumn(name = "family_id")
    private Family family; //expense -> family (Pentru ce familie?)

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location; //expense -> location (Unde?)

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL)
    private java.util.List<ExpenseItem> items; // Task: expense -> expense_items
}