package com.familie.cheltuieli_familie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RawInputDTO(
    @NotBlank(message = "Raw text is required and cannot be empty")
    @Size(max = 10000, message = "Raw text must not exceed 10000 characters")
    String rawText
) {}
