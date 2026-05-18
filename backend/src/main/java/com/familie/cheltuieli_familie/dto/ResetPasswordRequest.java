package com.familie.cheltuieli_familie.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest extends SecurityQuestionsRequest {
    @NotBlank(message = "New password is required")
    private String newPassword;
}
