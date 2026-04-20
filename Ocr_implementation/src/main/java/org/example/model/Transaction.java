package org.example.model;

public class Transaction
{
    private String date;
    private double sum;
    private String description;
    private String type;
    private String currency;

    public Transaction(String date, double sum, String description, String type, String currency) {
        this.date = date;
        this.sum = sum;
        this.description = description;
        this.type = type;
        this.currency = currency;
    }

    public void setDate(String date)
    {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }

    public double getSum() {
        return sum;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public String toString() {
        return "Transaction{" + "data este '" + date + '\'' + ", suma este " + sum +
                ", descrierea este'" + description + '\'' + ", tipul este '" + type + '\'' +
                ", currency este'" + currency + '\'' + '}';
    }
}