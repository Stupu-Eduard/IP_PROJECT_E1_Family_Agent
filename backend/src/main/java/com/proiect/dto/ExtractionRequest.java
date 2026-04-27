package com.proiect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionRequest {

    @NotBlank(message = "Raw text is required and cannot be empty")
    private String rawText;
}
