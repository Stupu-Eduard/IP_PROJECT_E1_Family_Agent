package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.ExpenseListDto;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.LocationRepository;
import org.junit.jupiter.api.Test;

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

        @Override public Long getId() { return id; }
        @Override public BigDecimal getAmount() { return amount; }
        @Override public String getCurrency() { return currency; }
        @Override public String getDescription() { return description; }
        @Override public LocalDateTime getExpenseDate() { return expenseDate; }
        @Override public String getCategory() { return category; }
        @Override public String getPerson() { return person; }
        @Override public Long getLocationId() { return locationId; }
        @Override public String getStore() { return store; }
        @Override public String getAddress() { return address; }
        @Override public String getCity() { return city; }
        @Override public String getCountry() { return country; }
        @Override public Double getLat() { return lat; }
        @Override public Double getLng() { return lng; }
    }

    private ExpenseController buildController(ExpenseRepository expenseRepository) {
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        return new ExpenseController(expenseRepository, categoryRepository, locationRepository);
    }

    @Test
    void list_blankFilters_becomeNull_andMapsLocation() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = buildController(expenseRepository);

        LocalDate date = LocalDate.of(2026, 4, 20);
        LocalDateTime expenseDate = LocalDateTime.of(2026, 4, 20, 10, 30);

        Projection row = new Projection(
                10L, BigDecimal.valueOf(12.50), "RON", "coffee",
                expenseDate, "Food", "Alex",
                7L, "Store X", "Street 1", "Cluj", "RO", 46.77, 23.59
        );

        when(expenseRepository.findAllWithLocationFiltered(eq(date), any(), any()))
                .thenReturn(List.of(row));

        List<ExpenseListDto> result = controller.list(date, "   ", "");

        verify(expenseRepository).findAllWithLocationFiltered(eq(date), isNull(), isNull());
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
        assertEquals("Street 1", dto.location().address());
        assertEquals("Cluj", dto.location().city());
        assertEquals("RO", dto.location().country());
        assertEquals(46.77, dto.location().lat());
        assertEquals(23.59, dto.location().lng());
    }

    @Test
    void list_withoutLocation_returnsNullLocation() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = buildController(expenseRepository);

        Projection row = new Projection(
                1L, BigDecimal.ONE, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                null, null, null, null, null, null, null, null, null
        );

        when(expenseRepository.findAllWithLocationFiltered(isNull(), isNull(), isNull()))
                .thenReturn(List.of(row));

        List<ExpenseListDto> result = controller.list(null, null, null);

        assertEquals(1, result.size());
        assertNull(result.getFirst().location());
    }

    @Test
    void getById_whenNotFound_throwsIllegalArgumentException() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = buildController(expenseRepository);

        when(expenseRepository.findOneWithLocation(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> controller.getById(99L));
    }

    @Test
    void getById_whenFound_mapsToDto() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = buildController(expenseRepository);

        Projection row = new Projection(
                2L, BigDecimal.TEN, "RON", "desc",
                LocalDateTime.of(2026, 2, 2, 12, 0),
                "Utilities", "Family",
                null, null, null, null, null, null, null
        );

        when(expenseRepository.findOneWithLocation(2L)).thenReturn(row);

        ExpenseListDto dto = controller.getById(2L);

        assertEquals(2L, dto.id());
        assertEquals(BigDecimal.TEN, dto.amount());
        assertEquals("Utilities", dto.category());
        assertNull(dto.location());
    }
}