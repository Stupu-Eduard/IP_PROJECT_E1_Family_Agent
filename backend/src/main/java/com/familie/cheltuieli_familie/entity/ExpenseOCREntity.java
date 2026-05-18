package com.familie.cheltuieli_familie.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
public class ExpenseOCREntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "expense_date")
    private LocalDateTime date;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "family_id")
    private Long familyId;

    private String currency;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "receipt_url")
    private String receiptUrl;

    public ExpenseOCREntity() {
    }

    public ExpenseOCREntity(BigDecimal amount, String description, LocalDateTime date,
                            String currency, String transactionType, String sourceType) {
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.currency = currency;
        this.transactionType = transactionType;
        this.sourceType = sourceType;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public String getCurrency() {
        return currency;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }
}