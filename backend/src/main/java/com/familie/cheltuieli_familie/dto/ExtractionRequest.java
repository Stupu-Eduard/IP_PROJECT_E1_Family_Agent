package com.familie.cheltuieli_familie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionRequest {

    @NotBlank(message = "Raw text is required and cannot be empty")
    @Size(max = 10000, message = "Raw text must not exceed 10000 characters")
    private String rawText;
}
