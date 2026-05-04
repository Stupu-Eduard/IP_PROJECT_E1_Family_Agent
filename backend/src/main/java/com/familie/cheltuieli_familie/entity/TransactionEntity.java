package com.familie.cheltuieli_familie.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_date")
    private String date;

    @Column(name = "amount")
    private double amount;

    @Column(name = "description")
    private String description;

    @Column(name = "transaction_type")
    private String type;

    @Column(name = "currency")
    private String currency;

    public TransactionEntity() {
    }

    public TransactionEntity(String date, double amount, String description, String type, String currency) {
        this.date = date;
        this.amount = amount;
        this.description = description;
        this.type = type;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }
}