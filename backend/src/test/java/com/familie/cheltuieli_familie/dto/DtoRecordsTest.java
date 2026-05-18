package com.familie.cheltuieli_familie.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
                "manual",
                null
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

    @Test
    void latLngDto_accessorsAndEquality() {
        LatLngDto a = new LatLngDto(44.4268, 26.1025);
        LatLngDto b = new LatLngDto(44.4268, 26.1025);

        assertEquals(44.4268, a.lat());
        assertEquals(26.1025, a.lng());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotNull(a.toString());
    }

    @Test
    void saveGeofenceZoneRequest_accessors() {
        List<LatLngDto> coords = List.of(new LatLngDto(44.0, 26.0), new LatLngDto(44.1, 26.1));
        SaveGeofenceZoneRequest req = new SaveGeofenceZoneRequest(coords, "Zona Test");

        assertEquals(coords, req.coordinates());
        assertEquals("Zona Test", req.name());
        assertNotNull(req.toString());
    }

    @Test
    void saveGeofenceZoneRequest_nullName() {
        List<LatLngDto> coords = List.of(new LatLngDto(44.0, 26.0));
        SaveGeofenceZoneRequest req = new SaveGeofenceZoneRequest(coords, null);

        assertNull(req.name());
    }

    @Test
    void geofenceZoneResponseDto_accessorsAndEquality() {
        List<LatLngDto> coords = List.of(new LatLngDto(44.0, 26.0));
        GeofenceZoneResponseDto a = new GeofenceZoneResponseDto(1L, "Safe Zone", coords);
        GeofenceZoneResponseDto b = new GeofenceZoneResponseDto(1L, "Safe Zone", coords);

        assertEquals(1L, a.id());
        assertEquals("Safe Zone", a.name());
        assertEquals(coords, a.coordinates());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotNull(a.toString());
    }
}
