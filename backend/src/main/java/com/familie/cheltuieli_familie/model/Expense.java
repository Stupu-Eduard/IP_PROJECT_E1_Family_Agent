package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "expenses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount; // Am schimbat din 'sum'
    private String description;

    @Column(name = "expense_date")
    private java.time.LocalDateTime expenseDate; // Task: expense_date, rename

    @Column(length = 10)
    private String currency = "RON"; // Task: currency, se pune ron default dar se poate schimba

    @Column(name = "source_type", length = 20)
    private String sourceType = "manual"; // Task: source_type (manual / OCR)

    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now(); // Task: created_at

    @Column(name = "raw_input", length = 1000)
    private String rawInput;

    @Column(name = "ai_category")
    private String aiCategory;

    @Column(name = "ai_location")
    private String aiLocation;

    @Column(name = "ai_person")
    private String aiPerson;

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
