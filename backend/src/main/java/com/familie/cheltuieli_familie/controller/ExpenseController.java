package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.CreateExpenseRequest;
import com.familie.cheltuieli_familie.dto.ExpenseListDto;
import com.familie.cheltuieli_familie.dto.LocationDto;
import com.familie.cheltuieli_familie.event.ExpenseSyncEvent;
import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.model.Location;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.LocationRepository;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
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

    private static final String ROLE_PARENT    = "ROLE_PARENT";
    private static final String ROLE_CO_PARENT = "ROLE_CO-PARENT";
    private static final String SOURCE_MANUAL  = "manual";

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final LocationRepository locationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ExpenseController(ExpenseRepository expenseRepository,
                             CategoryRepository categoryRepository,
                             FamilyMemberRepository familyMemberRepository,
                             LocationRepository locationRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.locationRepository = locationRepository;
        this.eventPublisher = eventPublisher;
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

        boolean isParent = isParent(auth);

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
        expense.setSourceType(SOURCE_MANUAL);

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

        // Sync to Qdrant vector store for semantic / RAG search
        ExpenseEntity entity = toExpenseEntity(saved);
        eventPublisher.publishEvent(new ExpenseSyncEvent(this, entity));

        return toDto(expenseRepository.findOneWithLocation(saved.getId()));
    }

    @GetMapping("/{id}")
    public ExpenseListDto getById(@PathVariable Long id) {
        ExpenseRepository.ExpenseWithLocationProjection row = expenseRepository.findOneWithLocation(id);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cheltuiala nu a fost găsită.");
        }
        return toDto(row);
    }

    @PutMapping("/{id}")
    public ExpenseListDto update(@PathVariable Long id,
                                 @Valid @RequestBody CreateExpenseRequest request,
                                 Authentication auth) {
        User user = (User) auth.getPrincipal();

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cheltuiala nu a fost găsită."));

        boolean isChild = isChildByDb(user);

        if (isChild) {
            if (!SOURCE_MANUAL.equals(expense.getSourceType())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Copiii pot edita doar tranzacțiile manuale.");
            }
            if (expense.getUser() == null || !expense.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Nu poți edita cheltuiala altei persoane.");
            }
        } else {
            checkFamilyAccess(user, expense);
        }

        Category category = categoryRepository.findByName(request.getCategoryName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Categoria '" + request.getCategoryName() + "' nu există."));

        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getDate().atStartOfDay());
        expense.setCategory(category);

        if (request.getStoreName() != null && !request.getStoreName().isBlank()) {
            Location location = expense.getLocation() != null ? expense.getLocation() : new Location();
            location.setStore(request.getStoreName().trim());
            location.setCity(request.getCity() != null && !request.getCity().isBlank()
                    ? request.getCity().trim() : null);
            expense.setLocation(locationRepository.save(location));
        }

        Expense saved = expenseRepository.save(expense);
        return toDto(expenseRepository.findOneWithLocation(saved.getId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        User user = (User) auth.getPrincipal();

        if (isChildByDb(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Copiii nu pot șterge tranzacții.");
        }

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cheltuiala nu a fost găsită."));

        checkFamilyAccess(user, expense);
        expenseRepository.deleteById(id);
    }

    private boolean isParent(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(ROLE_PARENT)
                        || a.getAuthority().equals(ROLE_CO_PARENT));
    }

    private boolean isChildByDb(User user) {
        return familyMemberRepository.findByUserId(user.getId()).stream()
                .anyMatch(fm -> "Child".equalsIgnoreCase(fm.getRole()));
    }

    private void checkFamilyAccess(User user, Expense expense) {
        boolean ownExpense = expense.getUser() != null
                && expense.getUser().getId().equals(user.getId());
        boolean sameFamily = expense.getFamily() != null
                && familyMemberRepository.existsByFamilyIdAndUserId(
                        expense.getFamily().getId(), user.getId());
        if (!ownExpense && !sameFamily) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ai acces la această cheltuială.");
        }
    }

    private ExpenseEntity toExpenseEntity(Expense expense) {
        ExpenseEntity entity = new ExpenseEntity();
        entity.setId(expense.getId());
        entity.setAmount(expense.getAmount());
        entity.setCategory(expense.getCategory() != null ? expense.getCategory().getName() : null);
        entity.setLocation(expense.getLocation() != null ? expense.getLocation().getStore() : null);
        entity.setPerson(expense.getUser() != null ? expense.getUser().getName() : null);
        entity.setDate(expense.getExpenseDate() != null ? expense.getExpenseDate().toLocalDate() : null);
        String rawInput = expense.getRawInput();
        if (rawInput == null || rawInput.isBlank()) {
            rawInput = String.format("Cheltuială manuală: %s, Sumă: %s RON, Categorie: %s, Magazin: %s, Persoană: %s",
                    expense.getDescription(), expense.getAmount(),
                    expense.getCategory() != null ? expense.getCategory().getName() : null,
                    expense.getLocation() != null ? expense.getLocation().getStore() : null,
                    expense.getUser() != null ? expense.getUser().getName() : null);
        }
        entity.setRawInput(rawInput);
        entity.setCreatedAt(expense.getCreatedAt() != null ? expense.getCreatedAt() : LocalDateTime.now());
        return entity;
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
                locationDto,
                row.getSourceType()
        );
    }
}
