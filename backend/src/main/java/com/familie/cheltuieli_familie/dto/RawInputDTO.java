package com.familie.cheltuieli_familie.dto;

import jakarta.validation.constraints.NotBlank;

public record RawInputDTO(@NotBlank(message = "Raw text is required and cannot be empty") String rawText) {
}
