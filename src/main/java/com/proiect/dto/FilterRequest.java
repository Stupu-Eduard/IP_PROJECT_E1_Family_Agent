package com.proiect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterRequest {
    private String query;
    private int topK = 5;
    private String category;
    private String person;
    private LocalDate from;
    private LocalDate to;
}
