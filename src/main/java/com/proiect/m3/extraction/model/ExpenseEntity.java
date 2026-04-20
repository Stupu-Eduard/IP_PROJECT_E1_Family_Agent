package com.proiect.m3.extraction.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
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

    private LocalDateTime date;

    @Column(name = "raw_input", length = 1000)
    private String rawInput;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ExpenseEntity() {}

    public ExpenseEntity(Long id, BigDecimal amount, String category, String location, String person, LocalDateTime transactionDate, LocalDateTime date, String rawInput, LocalDateTime createdAt) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.location = location;
        this.person = person;
        this.transactionDate = transactionDate;
        this.date = date;
        this.rawInput = rawInput;
        this.createdAt = createdAt;
    }

    public static ExpenseEntityBuilder builder() {
        return new ExpenseEntityBuilder();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPerson() { return person; }
    public void setPerson(String person) { this.person = person; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getRawInput() { return rawInput; }
    public void setRawInput(String rawInput) { this.rawInput = rawInput; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class ExpenseEntityBuilder {
        private Long id;
        private BigDecimal amount;
        private String category;
        private String location;
        private String person;
        private LocalDateTime transactionDate;
        private LocalDateTime date;
        private String rawInput;
        private LocalDateTime createdAt = LocalDateTime.now();

        public ExpenseEntityBuilder id(Long id) { this.id = id; return this; }
        public ExpenseEntityBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public ExpenseEntityBuilder category(String category) { this.category = category; return this; }
        public ExpenseEntityBuilder location(String location) { this.location = location; return this; }
        public ExpenseEntityBuilder person(String person) { this.person = person; return this; }
        public ExpenseEntityBuilder transactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; return this; }
        public ExpenseEntityBuilder date(LocalDateTime date) { this.date = date; return this; }
        public ExpenseEntityBuilder rawInput(String rawInput) { this.rawInput = rawInput; return this; }
        public ExpenseEntityBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ExpenseEntity build() {
            return new ExpenseEntity(id, amount, category, location, person, transactionDate, date, rawInput, createdAt);
        }
    }
}
