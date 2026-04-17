package com.familie.cheltuieli_familie.security.model;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class SecurityAlertDto {

    private final Long childId;
    private final Long parentId;
    private final String alertMessage;
    private final String restrictedCategory;
    private final LocalDateTime timestamp;

    public SecurityAlertDto(Long childId, Long parentId, String alertMessage, String restrictedCategory) {
        this.childId = childId;
        this.parentId = parentId;
        this.alertMessage = alertMessage;
        this.restrictedCategory = restrictedCategory;
        this.timestamp = LocalDateTime.now();
    }
}