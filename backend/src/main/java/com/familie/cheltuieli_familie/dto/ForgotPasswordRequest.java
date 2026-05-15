package com.familie.cheltuieli_familie.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Question 1 is required")
    private SecurityQuestion question1;

    @NotBlank(message = "Answer 1 is required")
    private String answer1;

    @NotNull(message = "Question 2 is required")
    private SecurityQuestion question2;

    @NotBlank(message = "Answer 2 is required")
    private String answer2;
}
