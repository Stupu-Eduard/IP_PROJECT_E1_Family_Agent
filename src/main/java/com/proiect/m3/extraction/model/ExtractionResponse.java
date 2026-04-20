package com.proiect.m3.extraction.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExtractionResponse {
    private BigDecimal amount;
    private String category;
    private String location;
    private String person;
    private LocalDateTime transactionDate;
    private String rawInput;

    public ExtractionResponse() {}

    public ExtractionResponse(BigDecimal amount, String category, String location, String person, LocalDateTime transactionDate, String rawInput) {
        this.amount = amount;
        this.category = category;
        this.location = location;
        this.person = person;
        this.transactionDate = transactionDate;
        this.rawInput = rawInput;
    }

    public static ExtractionResponseBuilder builder() {
        return new ExtractionResponseBuilder();
    }

    // Getters and Setters
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
    public String getRawInput() { return rawInput; }
    public void setRawInput(String rawInput) { this.rawInput = rawInput; }

    public static class ExtractionResponseBuilder {
        private BigDecimal amount;
        private String category;
        private String location;
        private String person;
        private LocalDateTime transactionDate;
        private String rawInput;

        public ExtractionResponseBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public ExtractionResponseBuilder category(String category) { this.category = category; return this; }
        public ExtractionResponseBuilder location(String location) { this.location = location; return this; }
        public ExtractionResponseBuilder person(String person) { this.person = person; return this; }
        public ExtractionResponseBuilder transactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; return this; }
        public ExtractionResponseBuilder rawInput(String rawInput) { this.rawInput = rawInput; return this; }

        public ExtractionResponse build() {
            return new ExtractionResponse(amount, category, location, person, transactionDate, rawInput);
        }
    }
}
