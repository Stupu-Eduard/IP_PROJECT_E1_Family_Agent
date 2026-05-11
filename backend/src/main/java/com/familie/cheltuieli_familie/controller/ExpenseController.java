package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.ExpenseListDto;
import com.familie.cheltuieli_familie.dto.LocationDto;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
@CrossOrigin(origins = {"https://family-agent.me", "http://localhost:5173"})
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final com.familie.cheltuieli_familie.service.QdrantVectorService qdrantVectorService;

    public ExpenseController(ExpenseRepository expenseRepository, com.familie.cheltuieli_familie.service.QdrantVectorService qdrantVectorService) {
        this.expenseRepository = expenseRepository;
        this.qdrantVectorService = qdrantVectorService;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!expenseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        expenseRepository.deleteById(id);
        qdrantVectorService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    private ExpenseListDto toDto(ExpenseRepository.ExpenseWithLocationProjection row) {
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
                locationDto
        );
    }
}
