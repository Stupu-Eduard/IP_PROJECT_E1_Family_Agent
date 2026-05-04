package com.familie.cheltuieli_familie.model;

public class Transaction {
    private String date;
    private double amount;
    private String description;
    private String type;
    private String currency;

    public Transaction() {
    }

    public Transaction(String date, double amount, String description, String type, String currency) {
        this.date = date;
        this.amount = amount;
        this.description = description;
        this.type = type;
        this.currency = currency;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "Transaction{" + "data este '" + date + '\'' + ", suma este " + amount +
                ", description='" + description + '\'' + ", type='" + type + '\'' +
                ", currency='" + currency + '\'' + '}';
    }
}