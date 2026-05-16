package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.CreateExpenseRequest;
import com.familie.cheltuieli_familie.dto.ExpenseListDto;
import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.Location;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExpenseControllerTest {

    private record Projection(Long id, BigDecimal amount, String currency, String description,
                              LocalDateTime expenseDate, String category, String person, String sourceType,
                              Long locationId, String store,
                              String address, String city, String country, Double lat,
                              Double lng) implements ExpenseRepository.ExpenseWithLocationProjection {

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public BigDecimal getAmount() {
            return amount;
        }

        @Override
        public String getCurrency() {
            return currency;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public LocalDateTime getExpenseDate() {
            return expenseDate;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getPerson() {
            return person;
        }

        @Override
        public String getSourceType() {
            return sourceType;
        }

        @Override
        public Long getLocationId() {
            return locationId;
        }

        @Override
        public String getStore() {
            return store;
        }

        @Override
        public String getAddress() {
            return address;
        }

        @Override
        public String getCity() {
            return city;
        }

        @Override
        public String getCountry() {
            return country;
        }

        @Override
        public Double getLat() {
            return lat;
        }

        @Override
        public Double getLng() {
            return lng;
        }
    }

    private Authentication parentAuth(User user) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(auth.getAuthorities()).thenAnswer(i ->
                List.of(new SimpleGrantedAuthority("ROLE_PARENT")));
        return auth;
    }

    private User mockUser(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private FamilyMember mockMembership(Long familyId) {
        Family family = mock(Family.class);
        when(family.getId()).thenReturn(familyId);
        FamilyMember fm = mock(FamilyMember.class);
        when(fm.getFamily()).thenReturn(family);
        return fm;
    }

    @Test
    void list_blankFilters_becomeNull_andMapsLocation() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        FamilyMember membership10 = mockMembership(10L);
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(membership10));

        LocalDate date = LocalDate.of(2026, 4, 20);
        LocalDateTime expenseDate = LocalDateTime.of(2026, 4, 20, 10, 30);

        Projection row = new Projection(
                10L, BigDecimal.valueOf(12.50), "RON", "coffee",
                expenseDate, "Food", "Alex", "manual",
                7L, "Store X", "Street 1", "Cluj", "RO", 46.77, 23.59
        );

        when(expenseRepository.findAllByFamilyFiltered(eq(10L), eq(date), isNull(), isNull()))
                .thenReturn(List.of(row));

        List<ExpenseListDto> result = controller.list(date, "   ", "", parentAuth(user));

        assertEquals(1, result.size());
        ExpenseListDto dto = result.getFirst();
        assertEquals(10L, dto.id());
        assertEquals(BigDecimal.valueOf(12.50), dto.amount());
        assertEquals("RON", dto.currency());
        assertEquals("coffee", dto.description());
        assertEquals(expenseDate, dto.expenseDate());
        assertEquals("Food", dto.category());
        assertEquals("Alex", dto.person());
        assertNotNull(dto.location());
        assertEquals(7L, dto.location().id());
        assertEquals("Store X", dto.location().store());
        assertEquals("Cluj", dto.location().city());
        assertEquals(46.77, dto.location().lat());
    }

    @Test
    void list_withoutLocation_returnsNullLocation() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(2L);
        FamilyMember membership5 = mockMembership(5L);
        when(familyMemberRepository.findByUserId(2L)).thenReturn(List.of(membership5));

        Projection row = new Projection(
                1L, BigDecimal.ONE, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                null, null, "manual", null, null, null, null, null, null, null
        );

        when(expenseRepository.findAllByFamilyFiltered(eq(5L), isNull(), isNull(), isNull()))
                .thenReturn(List.of(row));

        List<ExpenseListDto> result = controller.list(null, null, null, parentAuth(user));

        assertEquals(1, result.size());
        assertNull(result.getFirst().location());
    }

    @Test
    void getById_whenNotFound_throwsNotFound() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), mock(FamilyMemberRepository.class), mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        when(expenseRepository.findOneWithLocation(99L)).thenReturn(null);

        org.springframework.web.server.ResponseStatusException ex =
                assertThrows(org.springframework.web.server.ResponseStatusException.class,
                        () -> controller.getById(99L));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getById_whenFound_mapsToDto() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), mock(FamilyMemberRepository.class), mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        Projection row = new Projection(
                2L,
                BigDecimal.TEN,
                "RON",
                "desc",
                LocalDateTime.of(2026, 2, 2, 12, 0),
                "Utilities",
                "Family",
                "manual",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(expenseRepository.findOneWithLocation(2L)).thenReturn(row);

        ExpenseListDto dto = controller.getById(2L);

        assertEquals(2L, dto.id());
        assertEquals(BigDecimal.TEN, dto.amount());
        assertEquals("Utilities", dto.category());
        assertNull(dto.location());
    }

    // ── list: child and fallback branches ────────────────────────────────────

    @Test
    void list_childAuth_usesUserFilteredQuery() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(3L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(auth.getAuthorities()).thenAnswer(i -> List.of(new SimpleGrantedAuthority("ROLE_CHILD")));

        Projection row = new Projection(5L, BigDecimal.ONE, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, null, "manual", null, null, null, null, null, null, null);
        when(expenseRepository.findAllByUserFiltered(eq(3L), isNull(), isNull())).thenReturn(List.of(row));

        List<ExpenseListDto> result = controller.list(null, null, null, auth);

        assertEquals(1, result.size());
        assertEquals(5L, result.getFirst().id());
    }

    @Test
    void list_parentWithoutFamily_fallsBackToUserFiltered() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(4L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(auth.getAuthorities()).thenAnswer(i -> List.of(new SimpleGrantedAuthority("ROLE_PARENT")));
        when(familyMemberRepository.findByUserId(4L)).thenReturn(List.of());

        Projection row = new Projection(7L, BigDecimal.TEN, "RON", null,
                LocalDateTime.of(2026, 3, 1, 0, 0), null, null, "manual", null, null, null, null, null, null, null);
        when(expenseRepository.findAllByUserFiltered(eq(4L), isNull(), isNull())).thenReturn(List.of(row));

        List<ExpenseListDto> result = controller.list(null, null, null, auth);

        assertEquals(1, result.size());
        assertEquals(7L, result.getFirst().id());
    }

    @Test
    void list_nullAuth_returnsEmptyList() {
        ExpenseController controller = new ExpenseController(mock(ExpenseRepository.class), mock(CategoryRepository.class), mock(FamilyMemberRepository.class), mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        List<ExpenseListDto> result = controller.list(null, null, null, null);

        assertTrue(result.isEmpty());
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_withLocation_savesExpenseAndLocation() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, familyMemberRepository, locationRepository, mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());

        Location savedLocation = mock(Location.class);
        when(locationRepository.save(any(Location.class))).thenReturn(savedLocation);

        Expense savedExpense = mock(Expense.class);
        when(savedExpense.getId()).thenReturn(10L);
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        Projection row = new Projection(10L, BigDecimal.valueOf(50), "RON", "lunch",
                LocalDateTime.of(2026, 5, 1, 12, 0), "Food", "Alex", "manual",
                1L, "Restaurant", "Main St", "Cluj", "RO", 46.77, 23.59);
        when(expenseRepository.findOneWithLocation(10L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.valueOf(50));
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 5, 1));
        req.setStoreName("Restaurant");
        req.setCity("Cluj");

        ExpenseListDto result = controller.create(req, auth);

        assertEquals(10L, result.id());
        assertEquals("Food", result.category());
        assertNotNull(result.location());
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void create_withoutLocation_skipsLocationSave() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, familyMemberRepository, locationRepository, mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(2L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Transport")).thenReturn(Optional.of(category));
        when(familyMemberRepository.findByUserId(2L)).thenReturn(List.of());

        Expense savedExpense = mock(Expense.class);
        when(savedExpense.getId()).thenReturn(11L);
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        Projection row = new Projection(11L, BigDecimal.valueOf(20), "RON", null,
                LocalDateTime.of(2026, 5, 2, 8, 0), "Transport", "Alex", "manual",
                null, null, null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(11L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.valueOf(20));
        req.setCategoryName("Transport");
        req.setDate(LocalDate.of(2026, 5, 2));

        ExpenseListDto result = controller.create(req, auth);

        assertEquals(11L, result.id());
        assertNull(result.location());
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void create_categoryNotFound_throwsBadRequest() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, mock(FamilyMemberRepository.class), mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(3L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(categoryRepository.findByName("Inexistent")).thenReturn(Optional.empty());

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.valueOf(10));
        req.setCategoryName("Inexistent");
        req.setDate(LocalDate.of(2026, 5, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(req, auth));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void create_withFamily_setsExpenseFamily() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(4L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));

        Family family = mock(Family.class);
        FamilyMember fm = mock(FamilyMember.class);
        when(fm.getFamily()).thenReturn(family);
        when(familyMemberRepository.findByUserId(4L)).thenReturn(List.of(fm));

        Expense savedExpense = mock(Expense.class);
        when(savedExpense.getId()).thenReturn(12L);
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        Projection row = new Projection(12L, BigDecimal.valueOf(30), "RON", null,
                LocalDateTime.of(2026, 5, 3, 9, 0), "Food", "Alex", "manual",
                null, null, null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(12L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.valueOf(30));
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 5, 3));

        ExpenseListDto result = controller.create(req, auth);

        assertEquals(12L, result.id());
        verify(expenseRepository).save(any(Expense.class));
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_whenNotFound_throwsNotFound() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), mock(FamilyMemberRepository.class), mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(99L, req, auth));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void update_child_nonManualExpense_throwsForbidden() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        FamilyMember childMember = mock(FamilyMember.class);
        when(childMember.getRole()).thenReturn("Child");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(childMember));

        Expense expense = mock(Expense.class);
        when(expense.getSourceType()).thenReturn("ocr");
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(1L, req, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void update_child_anotherPersonExpense_throwsForbidden() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        FamilyMember childMember = mock(FamilyMember.class);
        when(childMember.getRole()).thenReturn("Child");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(childMember));

        User otherUser = mockUser(99L);
        Expense expense = mock(Expense.class);
        when(expense.getSourceType()).thenReturn("manual");
        when(expense.getUser()).thenReturn(otherUser);
        when(expenseRepository.findById(2L)).thenReturn(Optional.of(expense));

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(2L, req, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void update_child_nullOwner_throwsForbidden() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        FamilyMember childMember = mock(FamilyMember.class);
        when(childMember.getRole()).thenReturn("Child");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(childMember));

        Expense expense = mock(Expense.class);
        when(expense.getSourceType()).thenReturn("manual");
        when(expense.getUser()).thenReturn(null);
        when(expenseRepository.findById(3L)).thenReturn(Optional.of(expense));

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(3L, req, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void update_child_ownManualExpense_succeeds() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        FamilyMember childMember = mock(FamilyMember.class);
        when(childMember.getRole()).thenReturn("Child");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(childMember));

        Expense expense = mock(Expense.class);
        when(expense.getSourceType()).thenReturn("manual");
        when(expense.getUser()).thenReturn(user);
        when(expense.getId()).thenReturn(4L);
        when(expenseRepository.findById(4L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(expense)).thenReturn(expense);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));

        Projection row = new Projection(4L, BigDecimal.ONE, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0), "Food", null, "manual",
                null, null, null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(4L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ExpenseListDto result = controller.update(4L, req, auth);
        assertEquals(4L, result.id());
    }

    @Test
    void update_parent_ownExpense_succeeds() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(5L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(5L)).thenReturn(List.of());

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(user);
        when(expense.getId()).thenReturn(5L);
        when(expenseRepository.findById(5L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(expense)).thenReturn(expense);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));

        Projection row = new Projection(5L, BigDecimal.TEN, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0), "Food", null, "manual",
                null, null, null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(5L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.TEN);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ExpenseListDto result = controller.update(5L, req, auth);
        assertEquals(5L, result.id());
    }

    @Test
    void update_parent_familyExpense_succeeds() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(6L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(6L)).thenReturn(List.of());

        User otherUser = mockUser(99L);
        Family family = mock(Family.class);
        when(family.getId()).thenReturn(20L);

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(otherUser);
        when(expense.getFamily()).thenReturn(family);
        when(expense.getId()).thenReturn(6L);
        when(expenseRepository.findById(6L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(expense)).thenReturn(expense);
        when(familyMemberRepository.existsByFamilyIdAndUserId(20L, 6L)).thenReturn(true);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));

        Projection row = new Projection(6L, BigDecimal.TEN, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0), "Food", null, "manual",
                null, null, null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(6L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.TEN);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ExpenseListDto result = controller.update(6L, req, auth);
        assertEquals(6L, result.id());
    }

    @Test
    void update_parent_noAccess_throwsForbidden() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(7L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(7L)).thenReturn(List.of());

        User otherUser = mockUser(99L);
        Family family = mock(Family.class);
        when(family.getId()).thenReturn(30L);

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(otherUser);
        when(expense.getFamily()).thenReturn(family);
        when(expenseRepository.findById(7L)).thenReturn(Optional.of(expense));
        when(familyMemberRepository.existsByFamilyIdAndUserId(30L, 7L)).thenReturn(false);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(7L, req, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void update_parent_noFamilyOnExpense_throwsForbidden() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(8L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(8L)).thenReturn(List.of());

        User otherUser = mockUser(99L);
        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(otherUser);
        when(expense.getFamily()).thenReturn(null);
        when(expenseRepository.findById(8L)).thenReturn(Optional.of(expense));

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(8L, req, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void update_categoryNotFound_throwsBadRequest() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(9L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(9L)).thenReturn(List.of());

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(user);
        when(expenseRepository.findById(9L)).thenReturn(Optional.of(expense));
        when(categoryRepository.findByName("Unknown")).thenReturn(Optional.empty());

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Unknown");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(9L, req, auth));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void update_withExistingLocation_updatesInPlace() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, familyMemberRepository, locationRepository, mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(10L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(10L)).thenReturn(List.of());

        Location existingLocation = mock(Location.class);
        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(user);
        when(expense.getLocation()).thenReturn(existingLocation);
        when(expense.getId()).thenReturn(10L);
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(expense)).thenReturn(expense);
        when(locationRepository.save(existingLocation)).thenReturn(existingLocation);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));

        Projection row = new Projection(10L, BigDecimal.TEN, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0), "Food", null, "manual",
                1L, "Updated Store", null, "Cluj", null, null, null);
        when(expenseRepository.findOneWithLocation(10L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.TEN);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));
        req.setStoreName("Updated Store");
        req.setCity("Cluj");

        ExpenseListDto result = controller.update(10L, req, auth);
        assertEquals(10L, result.id());
        verify(locationRepository).save(existingLocation);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_child_throwsForbidden() {
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(mock(ExpenseRepository.class), mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        FamilyMember childMember = mock(FamilyMember.class);
        when(childMember.getRole()).thenReturn("Child");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(childMember));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.delete(1L, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void delete_whenNotFound_throwsNotFound() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(2L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(2L)).thenReturn(List.of());
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.delete(99L, auth));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void delete_parent_ownExpense_succeeds() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(3L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(3L)).thenReturn(List.of());

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(user);
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(expense));

        assertDoesNotThrow(() -> controller.delete(10L, auth));
        verify(expenseRepository).deleteById(10L);
    }

    @Test
    void delete_parent_familyExpense_succeeds() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(4L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(4L)).thenReturn(List.of());

        User otherUser = mockUser(99L);
        Family family = mock(Family.class);
        when(family.getId()).thenReturn(40L);

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(otherUser);
        when(expense.getFamily()).thenReturn(family);
        when(expenseRepository.findById(11L)).thenReturn(Optional.of(expense));
        when(familyMemberRepository.existsByFamilyIdAndUserId(40L, 4L)).thenReturn(true);

        assertDoesNotThrow(() -> controller.delete(11L, auth));
        verify(expenseRepository).deleteById(11L);
    }

    @Test
    void delete_parent_noAccess_throwsForbidden() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(5L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(5L)).thenReturn(List.of());

        User otherUser = mockUser(99L);
        Family family = mock(Family.class);
        when(family.getId()).thenReturn(50L);

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(otherUser);
        when(expense.getFamily()).thenReturn(family);
        when(expenseRepository.findById(12L)).thenReturn(Optional.of(expense));
        when(familyMemberRepository.existsByFamilyIdAndUserId(50L, 5L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.delete(12L, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void list_withCoParentRole_worksLikeParent() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(auth.getAuthorities()).thenAnswer(i -> List.of(new SimpleGrantedAuthority("ROLE_CO-PARENT")));

        FamilyMember membership = mockMembership(100L);
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(membership));

        when(expenseRepository.findAllByFamilyFiltered(eq(100L), any(), any(), any())).thenReturn(List.of());

        controller.list(null, null, null, auth);
        verify(expenseRepository).findAllByFamilyFiltered(eq(100L), any(), any(), any());
    }

    @Test
    void list_principalNotUser_returnsEmpty() {
        ExpenseController controller = new ExpenseController(mock(ExpenseRepository.class), mock(CategoryRepository.class), mock(FamilyMemberRepository.class), mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("NotAUser");

        List<ExpenseListDto> result = controller.list(null, null, null, auth);
        assertTrue(result.isEmpty());
    }

    @Test
    void list_withNonBlankFilters_passesFiltersToRepository() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        FamilyMember membership = mockMembership(10L);
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(membership));

        LocalDate date = LocalDate.of(2026, 4, 20);
        when(expenseRepository.findAllByFamilyFiltered(anyLong(), any(), any(), any())).thenReturn(List.of());

        controller.list(date, "Food", "Alex", parentAuth(user));

        verify(expenseRepository).findAllByFamilyFiltered(10L, date, "Food", "Alex");
    }

    @Test
    void create_withBlankStore_doesNotSaveLocation() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, mock(FamilyMemberRepository.class), locationRepository, mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(mock(Category.class)));

        Expense savedExpense = mock(Expense.class);
        when(savedExpense.getId()).thenReturn(1L);
        when(expenseRepository.save(any())).thenReturn(savedExpense);
        
        Projection row = new Projection(1L, BigDecimal.TEN, "RON", "desc", LocalDateTime.now(), "Food", "Alex", "manual", null, null, null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(1L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setCategoryName("Food");
        req.setAmount(BigDecimal.TEN);
        req.setDate(LocalDate.now());
        req.setStoreName("  ");

        controller.create(req, auth);
        verify(locationRepository, never()).save(any());
    }

    @Test
    void update_addingNewLocation_savesSuccessfully() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, mock(FamilyMemberRepository.class), locationRepository, mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        Expense expense = new Expense();
        expense.setUser(user);
        expense.setLocation(null);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any())).thenReturn(expense);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(mock(Category.class)));
        when(locationRepository.save(any())).thenReturn(new Location());

        Projection row = new Projection(1L, BigDecimal.TEN, "RON", "desc", LocalDateTime.now(), "Food", "Alex", "manual", 10L, "New Store", null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(any())).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setCategoryName("Food");
        req.setAmount(BigDecimal.TEN);
        req.setDate(LocalDate.now());
        req.setStoreName("New Store");

        controller.update(1L, req, auth);
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void update_withNullCity_works() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, mock(FamilyMemberRepository.class), locationRepository, mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        Location loc = new Location();
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setLocation(loc);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any())).thenReturn(expense);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(mock(Category.class)));
        when(locationRepository.save(any())).thenReturn(loc);

        Projection row = new Projection(1L, BigDecimal.TEN, "RON", "desc", LocalDateTime.now(), "Food", "Alex", "manual", 10L, "Store", null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(any())).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setCategoryName("Food");
        req.setAmount(BigDecimal.TEN);
        req.setDate(LocalDate.now());
        req.setStoreName("Store");
        req.setCity(null);

        controller.update(1L, req, auth);
        verify(locationRepository).save(loc);
        assertNull(loc.getCity());
    }

    @Test
    void delete_coParent_succeeds() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(auth.getAuthorities()).thenAnswer(i -> List.of(new SimpleGrantedAuthority("ROLE_CO-PARENT")));
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());

        Expense expense = new Expense();
        expense.setUser(user);
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(expense));

        assertDoesNotThrow(() -> controller.delete(10L, auth));
        verify(expenseRepository).deleteById(10L);
    }

    @Test
    void toDto_mapsAllFieldsCorrectly() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), mock(FamilyMemberRepository.class), mock(LocationRepository.class), mock(org.springframework.context.ApplicationEventPublisher.class));

        LocalDateTime now = LocalDateTime.now();
        Projection row = new Projection(
                1L, BigDecimal.TEN, "USD", "Description", now, "Category", "Person", "source",
                100L, "Store", "Address", "City", "Country", 1.23, 4.56
        );

        when(expenseRepository.findOneWithLocation(1L)).thenReturn(row);

        ExpenseListDto dto = controller.getById(1L);

        assertEquals(1L, dto.id());
        assertEquals(BigDecimal.TEN, dto.amount());
        assertEquals("USD", dto.currency());
        assertEquals("Description", dto.description());
        assertEquals(now, dto.expenseDate());
        assertEquals("Category", dto.category());
        assertEquals("Person", dto.person());
        assertEquals("source", dto.sourceType());

        assertNotNull(dto.location());
        assertEquals(100L, dto.location().id());
        assertEquals("Store", dto.location().store());
        assertEquals("Address", dto.location().address());
        assertEquals("City", dto.location().city());
        assertEquals("Country", dto.location().country());
        assertEquals(1.23, dto.location().lat());
        assertEquals(4.56, dto.location().lng());
    }
}
