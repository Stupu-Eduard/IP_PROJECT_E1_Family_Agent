package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocationTest {

    @Test
    @DisplayName("Model: Should set and get ID")
    void testIdGetterSetter() {
        Location loc = new Location();
        loc.setId(1L);
        assertEquals(1L, loc.getId());
    }

    @Test
    @DisplayName("Model: Should set and get store name")
    void testStoreGetterSetter() {
        Location loc = new Location();
        loc.setStore("Kaufland");
        assertEquals("Kaufland", loc.getStore());
    }

    @Test
    @DisplayName("Model: Should set and get city")
    void testCityGetterSetter() {
        Location loc = new Location();
        loc.setCity("Iasi");
        assertEquals("Iasi", loc.getCity());
    }

    @Test
    @DisplayName("Model: Should set and get country")
    void testCountryGetterSetter() {
        Location loc = new Location();
        loc.setCountry("Romania");
        assertEquals("Romania", loc.getCountry());
    }

    @Test
    @DisplayName("Model: New location should have null fields")
    void testDefaultValues() {
        Location loc = new Location();
        assertNull(loc.getStore());
        assertNull(loc.getCity());
        assertNull(loc.getId());
    }
}