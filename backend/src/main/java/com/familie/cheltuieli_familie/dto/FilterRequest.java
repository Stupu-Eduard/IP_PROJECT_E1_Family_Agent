package com.familie.cheltuieli_familie.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterRequest {

    @NotBlank(message = "Query is required and cannot be empty")
    private String query;
    private int topK = 15;
    private String category;
    private String person;
    private LocalDate from;
    private LocalDate to;
}
