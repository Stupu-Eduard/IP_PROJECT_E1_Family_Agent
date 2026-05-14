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
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class));

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
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class));

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
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), mock(FamilyMemberRepository.class), mock(LocationRepository.class));

        when(expenseRepository.findOneWithLocation(99L)).thenReturn(null);

        org.springframework.web.server.ResponseStatusException ex =
                assertThrows(org.springframework.web.server.ResponseStatusException.class,
                        () -> controller.getById(99L));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getById_whenFound_mapsToDto() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), mock(FamilyMemberRepository.class), mock(LocationRepository.class));

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
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class));

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
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), familyMemberRepository, mock(LocationRepository.class));

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
        ExpenseController controller = new ExpenseController(mock(ExpenseRepository.class), mock(CategoryRepository.class), mock(FamilyMemberRepository.class), mock(LocationRepository.class));

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
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, familyMemberRepository, locationRepository);

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
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, familyMemberRepository, locationRepository);

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
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, mock(FamilyMemberRepository.class), mock(LocationRepository.class));

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
        ExpenseController controller = new ExpenseController(expenseRepository, categoryRepository, familyMemberRepository, mock(LocationRepository.class));

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
}
