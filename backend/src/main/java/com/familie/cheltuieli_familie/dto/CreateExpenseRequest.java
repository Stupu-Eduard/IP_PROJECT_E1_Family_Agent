package com.familie.cheltuieli_familie.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateExpenseRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "Suma trebuie să fie mai mare ca 0.")
    private BigDecimal amount;

    private String description;

    @NotBlank(message = "Categoria este obligatorie.")
    private String categoryName;

    @NotNull(message = "Data este obligatorie.")
    private LocalDate date;

    private String storeName;
    private String city;
    private String receiptUrl;
}
