package com.familie.cheltuieli_familie.model;

import java.io.Serializable;
import java.time.LocalDate;

public class Transaction implements Serializable {

    private LocalDate date;
    private String description;
    private double amount;
    private String currency = "RON";
    private String type = "EXPENSE";

    public Transaction() {
    }

    public Transaction(LocalDate date, String description, double amount) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.currency = "RON";
        this.type = "EXPENSE";
    }

    public Transaction(LocalDate date, String description, double amount, String currency, String type) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    @Override
    public String toString() {
        return "Transaction{" +
                "date=" + date +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}