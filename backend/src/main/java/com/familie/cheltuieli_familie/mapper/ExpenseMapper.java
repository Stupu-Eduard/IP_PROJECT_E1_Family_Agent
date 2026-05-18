package com.familie.cheltuieli_familie.mapper;

import com.familie.cheltuieli_familie.dto.ExpenseListDto;
import com.familie.cheltuieli_familie.dto.LocationDto;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ExpenseMapper {

    public ExpenseEntity toExpenseEntity(Expense expense) {
        ExpenseEntity entity = new ExpenseEntity();
        entity.setId(expense.getId());
        entity.setAmount(expense.getAmount());
        entity.setCategory(expense.getCategory() != null ? expense.getCategory().getName() : null);
        entity.setLocation(expense.getLocation() != null ? expense.getLocation().getStore() : null);
        entity.setPerson(expense.getUser() != null ? expense.getUser().getName() : null);
        entity.setDate(expense.getExpenseDate() != null ? expense.getExpenseDate().toLocalDate() : null);

        String rawInput = expense.getRawInput();
        if (rawInput == null || rawInput.isBlank()) {
            String source = expense.getSourceType() != null ? expense.getSourceType() : "manual";
            rawInput = String.format("Cheltuială %s: %s, Sumă: %s RON, Categorie: %s, Magazin: %s, Persoană: %s",
                    source,
                    expense.getDescription(),
                    expense.getAmount(),
                    expense.getCategory() != null ? expense.getCategory().getName() : "Fără categorie",
                    expense.getLocation() != null ? expense.getLocation().getStore() : "Fără locație",
                    expense.getUser() != null ? expense.getUser().getName() : "Necunoscut");
        }
        entity.setRawInput(rawInput);
        entity.setCreatedAt(expense.getCreatedAt() != null ? expense.getCreatedAt() : LocalDateTime.now());
        entity.setFamilyId(expense.getFamily() != null ? expense.getFamily().getId() : null);
        entity.setUserId(expense.getUser() != null ? expense.getUser().getId() : null);
        return entity;
    }

    public ExpenseListDto toDto(ExpenseRepository.ExpenseWithLocationProjection row) {
        LocationDto locationDto = null;
        if (row.getLocationId() != null) {
            locationDto = new LocationDto(
                    row.getLocationId(),
                    row.getStore(),
                    row.getAddress(),
                    row.getCity(),
                    row.getCountry(),
                    row.getLat(),
                    row.getLng()
            );
        }

        return new ExpenseListDto(
                row.getId(),
                row.getAmount(),
                row.getCurrency(),
                row.getDescription(),
                row.getExpenseDate(),
                row.getCategory(),
                row.getPerson(),
                locationDto,
                row.getSourceType()
        );
    }
}
