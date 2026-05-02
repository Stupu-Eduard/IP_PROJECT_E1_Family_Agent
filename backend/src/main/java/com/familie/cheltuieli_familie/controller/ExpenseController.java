package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.ExpenseListDto;
import com.familie.cheltuieli_familie.dto.LocationDto;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
@CrossOrigin(origins = {"https://family-agent.me", "http://localhost:5173"})
public class ExpenseController {

    private final ExpenseRepository expenseRepository;

    public ExpenseController(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @GetMapping
    public List<ExpenseListDto> list(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String person
    ) {
        String categoryFilter = (category == null || category.isBlank()) ? null : category;
        String personFilter = (person == null || person.isBlank()) ? null : person;

        return expenseRepository.findAllWithLocationFiltered(date, categoryFilter, personFilter)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public ExpenseListDto getById(@PathVariable Long id) {
        ExpenseRepository.ExpenseWithLocationProjection row = expenseRepository.findOneWithLocation(id);
        if (row == null) {
            throw new IllegalArgumentException("Expense not found");
        }

        return toDto(row);
    }

    private ExpenseListDto toDto(ExpenseRepository.ExpenseWithLocationProjection row) {
        LocationDto locationDto = null;
        if (row.locationId() != null) {
            locationDto = new LocationDto(
                    row.locationId(),
                    row.store(),
                    row.address(),
                    row.city(),
                    row.country(),
                    row.lat(),
                    row.lng()
            );
        }

        return new ExpenseListDto(
                row.id(),
                row.amount(),
                row.currency(),
                row.description(),
                row.expenseDate(),
                row.category(),
                row.person(),
                locationDto
        );
    }
}
