package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.CreateExpenseRequest;
import com.familie.cheltuieli_familie.dto.ExpenseListDto;
import com.familie.cheltuieli_familie.dto.LocationDto;
import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.Location;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.LocationRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
@CrossOrigin(origins = {"https://family-agent.me", "http://localhost:5173"})
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final LocationRepository locationRepository;

    public ExpenseController(ExpenseRepository expenseRepository,
                             CategoryRepository categoryRepository,
                             FamilyMemberRepository familyMemberRepository,
                             LocationRepository locationRepository) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.locationRepository = locationRepository;
    }

    @GetMapping
    public List<ExpenseListDto> list(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String person,
            Authentication auth
    ) {
        String categoryFilter = (category == null || category.isBlank()) ? null : category;
        String personFilter   = (person == null || person.isBlank()) ? null : person;

        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return List.of();
        }

        boolean isParent = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT")
                        || a.getAuthority().equals("ROLE_CO-PARENT"));

        if (isParent) {
            return familyMemberRepository.findByUserId(user.getId()).stream()
                    .findFirst()
                    .map(fm -> expenseRepository.findAllByFamilyFiltered(
                            fm.getFamily().getId(), date, categoryFilter, personFilter))
                    .orElseGet(() -> expenseRepository.findAllByUserFiltered(
                            user.getId(), date, categoryFilter))
                    .stream()
                    .map(this::toDto)
                    .toList();
        } else {
            return expenseRepository.findAllByUserFiltered(user.getId(), date, categoryFilter)
                    .stream()
                    .map(this::toDto)
                    .toList();
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseListDto create(@Valid @RequestBody CreateExpenseRequest request, Authentication auth) {
        User user = (User) auth.getPrincipal();

        Category category = categoryRepository.findByName(request.getCategoryName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Categoria '" + request.getCategoryName() + "' nu există."));

        Expense expense = new Expense();
        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getDate().atStartOfDay());
        expense.setCategory(category);
        expense.setUser(user);
        expense.setSourceType("manual");

        if (request.getStoreName() != null && !request.getStoreName().isBlank()) {
            Location location = new Location();
            location.setStore(request.getStoreName().trim());
            if (request.getCity() != null && !request.getCity().isBlank()) {
                location.setCity(request.getCity().trim());
            }
            expense.setLocation(locationRepository.save(location));
        }

        familyMemberRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .ifPresent(fm -> expense.setFamily(fm.getFamily()));

        Expense saved = expenseRepository.save(expense);

        ExpenseRepository.ExpenseWithLocationProjection row = expenseRepository.findOneWithLocation(saved.getId());
        return toDto(row);
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
