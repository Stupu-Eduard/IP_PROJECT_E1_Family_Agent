package com.familie.cheltuieli_familie.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DtoRecordsTest {

    @Test
    void locationDto_accessorsAndEquality() {
        LocationDto a = new LocationDto(1L, "Store", "Addr", "City", "RO", 1.2, 3.4);
        LocationDto b = new LocationDto(1L, "Store", "Addr", "City", "RO", 1.2, 3.4);

        assertEquals(1L, a.id());
        assertEquals("Store", a.store());
        assertEquals("Addr", a.address());
        assertEquals("City", a.city());
        assertEquals("RO", a.country());
        assertEquals(1.2, a.lat());
        assertEquals(3.4, a.lng());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotNull(a.toString());
    }

    @Test
    void expenseListDto_accessors() {
        LocalDateTime dt = LocalDateTime.of(2026, 4, 20, 9, 0);
        LocationDto loc = new LocationDto(9L, "Shop", null, "Buc", "RO", 44.4, 26.1);

        ExpenseListDto dto = new ExpenseListDto(
                100L,
                BigDecimal.valueOf(99.99),
                "RON",
                "desc",
                dt,
                "Food",
                "Family",
                loc,
                "manual"
        );

        assertEquals(100L, dto.id());
        assertEquals(BigDecimal.valueOf(99.99), dto.amount());
        assertEquals("RON", dto.currency());
        assertEquals("desc", dto.description());
        assertEquals(dt, dto.expenseDate());
        assertEquals("Food", dto.category());
        assertEquals("Family", dto.person());
        assertEquals(loc, dto.location());
        assertNotNull(dto.toString());
    }

    @Test
    void updateLocationCoordinatesRequest_accessors() {
        UpdateLocationCoordinatesRequest req = new UpdateLocationCoordinatesRequest(12.34, 56.78);
        assertEquals(12.34, req.lat());
        assertEquals(56.78, req.lng());
        assertNotNull(req.toString());
    }
}
