package com.familie.cheltuieli_familie.security.model;

import java.io.Serializable;

public class SecurityAlertDto implements Serializable {

    private Long childId;
    private Long parentId;
    private String alertMessage;
    private String restrictedCategory;
    private Long timestamp;

    // --- ACESTA ESTE CONSTRUCTORUL CARE REZOLVĂ EROAREA TA ---
    public SecurityAlertDto() {
    }

    // Constructorul complet
    public SecurityAlertDto(Long childId, Long parentId, String alertMessage, String restrictedCategory, Long timestamp) {
        this.childId = childId;
        this.parentId = parentId;
        this.alertMessage = alertMessage;
        this.restrictedCategory = restrictedCategory;
        this.timestamp = timestamp;
    }

    // --- GETTERS ȘI SETTERS ---

    public Long getChildId() {
        return childId;
    }

    public void setChildId(Long childId) {
        this.childId = childId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public String getRestrictedCategory() {
        return restrictedCategory;
    }

    public void setRestrictedCategory(String restrictedCategory) {
        this.restrictedCategory = restrictedCategory;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}