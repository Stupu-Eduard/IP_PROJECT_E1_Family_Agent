package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.ExpenseListDto;
import com.familie.cheltuieli_familie.dto.LocationDto;
import com.familie.cheltuieli_familie.dto.ManualExpenseRequest;
import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.Location;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.LocationRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
@CrossOrigin(origins = {"https://family-agent.me", "http://localhost:5173"})
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;

    public ExpenseController(ExpenseRepository expenseRepository,
                             CategoryRepository categoryRepository,
                             LocationRepository locationRepository) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
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

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ManualExpenseRequest request) {
        Expense expense = new Expense();
        expense.setAmount(request.getAmount());
        expense.setCurrency(request.getCurrency());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getDate() != null ? request.getDate() : LocalDateTime.now());
        expense.setSourceType("manual");
        expense.setCreatedAt(LocalDateTime.now());

        // Găsim sau creăm categoria
        Category category = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(request.getCategory()))
                .findFirst()
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName(request.getCategory());
                    c.setIsActive(true);
                    return categoryRepository.save(c);
                });
        expense.setCategory(category);

        // Dacă există locație
        if (request.getLocationName() != null && !request.getLocationName().isBlank()) {
            Location location = new Location();
            location.setStore(request.getLocationName());
            location = locationRepository.save(location);
            expense.setLocation(location);
        }

        expenseRepository.save(expense);
        return ResponseEntity.ok().build();
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