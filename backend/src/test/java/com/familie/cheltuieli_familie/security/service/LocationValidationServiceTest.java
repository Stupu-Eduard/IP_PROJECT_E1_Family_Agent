package com.familie.cheltuieli_familie.security.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Location Validation Service - Unit Tests")
class LocationValidationServiceTest {

    private final LocationValidationService validationService = new LocationValidationService();

    @Test
    @DisplayName("✅ Should return true for valid coordinates (Bucharest)")
    void testValidLocation() {
        boolean result = validationService.isLocationValid(44.4268, 26.1025);
        assertTrue(result, "O locație normală trebuie să fie validă.");
    }

    @Test
    @DisplayName("❌ Should return false for Null Island (0,0)")
    void testNullIsland() {
        boolean result = validationService.isLocationValid(0.0, 0.0);
        assertFalse(result, "Locația (0,0) este de obicei o eroare GPS și trebuie respinsă.");
    }

    @Test
    @DisplayName("❌ Should return false for latitude > 90")
    void testLatitudeAboveMax() {
        boolean result = validationService.isLocationValid(91.0, 26.0);
        assertFalse(result, "Latitudinea nu poate depăși 90 de grade.");
    }

    @Test
    @DisplayName("❌ Should return false for latitude < -90")
    void testLatitudeBelowMin() {
        boolean result = validationService.isLocationValid(-91.0, 26.0);
        assertFalse(result, "Latitudinea nu poate fi mai mică de -90 de grade.");
    }

    @Test
    @DisplayName("❌ Should return false for longitude > 180")
    void testLongitudeAboveMax() {
        boolean result = validationService.isLocationValid(44.0, 181.0);
        assertFalse(result, "Longitudinea nu poate depăși 180 de grade.");
    }

    @Test
    @DisplayName("❌ Should return false for longitude < -180")
    void testLongitudeBelowMin() {
        boolean result = validationService.isLocationValid(44.0, -181.0);
        assertFalse(result, "Longitudinea nu poate fi mai mică de -180 de grade.");
    }

    @Test
    @DisplayName("✅ Should return true if only one coordinate is zero")
    void testOnlyOneZeroCoordinate() {
        // Un punct pe Ecuator dar nu pe Meridianul Zero
        assertTrue(validationService.isLocationValid(0.0, 26.0), "Latitudinea 0 este validă dacă longitudinea nu e 0.");

        // Un punct pe Meridianul Zero dar nu pe Ecuator
        assertTrue(validationService.isLocationValid(44.0, 0.0), "Longitudinea 0 este validă dacă latitudinea nu e 0.");
    }

    @Test
    @DisplayName("✅ Should return true for exact boundary values")
    void testBoundaryValues() {
        // Testăm fix limitele admise
        assertTrue(validationService.isLocationValid(90.0, 180.0));
        assertTrue(validationService.isLocationValid(-90.0, -180.0));
    }
}