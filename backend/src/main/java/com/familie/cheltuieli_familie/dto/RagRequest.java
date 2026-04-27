package com.familie.cheltuieli_familie.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagRequest {

    @NotBlank(message = "Query is required and cannot be empty")
    private String query;
}
