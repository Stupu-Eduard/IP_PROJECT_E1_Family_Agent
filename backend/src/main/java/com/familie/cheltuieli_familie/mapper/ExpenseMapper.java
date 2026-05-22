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
        entity.setCategory(mapCategoryName(expense));
        entity.setLocation(mapLocationName(expense));
        entity.setPerson(mapPersonName(expense));
        entity.setDate(expense.getExpenseDate() != null ? expense.getExpenseDate().toLocalDate() : null);
        entity.setRawInput(buildRawInput(expense));
        entity.setCreatedAt(expense.getCreatedAt() != null ? expense.getCreatedAt() : LocalDateTime.now());
        entity.setFamilyId(mapFamilyId(expense));
        entity.setUserId(mapUserId(expense));
        return entity;
    }

    private String mapCategoryName(Expense expense) {
        return expense.getCategory() != null ? expense.getCategory().getName() : null;
    }

    private String mapLocationName(Expense expense) {
        return expense.getLocation() != null ? expense.getLocation().getStore() : null;
    }

    private String mapPersonName(Expense expense) {
        return expense.getUser() != null ? expense.getUser().getName() : null;
    }

    private String buildRawInput(Expense expense) {
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
        return rawInput;
    }

    private Long mapFamilyId(Expense expense) {
        return expense.getFamily() != null ? expense.getFamily().getId() : null;
    }

    private Long mapUserId(Expense expense) {
        return expense.getUser() != null ? expense.getUser().getId() : null;
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
                row.getSourceType(),
                row.getReceiptUrl()
        );
    }
}
