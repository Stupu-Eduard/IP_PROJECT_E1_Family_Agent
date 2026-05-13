package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.ExpenseListDto;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExpenseControllerTest {

    private record Projection(Long id, BigDecimal amount, String currency, String description,
                              LocalDateTime expenseDate, String category, String person, Long locationId, String store,
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
                expenseDate, "Food", "Alex",
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
                null, null, null, null, null, null, null, null, null
        );

        when(expenseRepository.findAllByFamilyFiltered(eq(5L), isNull(), isNull(), isNull()))
                .thenReturn(List.of(row));

        List<ExpenseListDto> result = controller.list(null, null, null, parentAuth(user));

        assertEquals(1, result.size());
        assertNull(result.getFirst().location());
    }

    @Test
    void getById_whenNotFound_throwsIllegalArgumentException() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = new ExpenseController(expenseRepository, mock(CategoryRepository.class), mock(FamilyMemberRepository.class), mock(LocationRepository.class));

        when(expenseRepository.findOneWithLocation(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> controller.getById(99L));
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
}
