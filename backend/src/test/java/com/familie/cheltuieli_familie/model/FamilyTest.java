package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FamilyTest {

    @Test
    @DisplayName("Model Unit Test: Should have null id before persistence")
    void testIdIsNullByDefault() {
        Family family = new Family();

        assertNull(family.getId());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly set and get name")
    void testNameGetterSetter() {
        Family family = new Family();
        family.setName("Familia Popescu");

        assertEquals("Familia Popescu", family.getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should have null name by default")
    void testNameIsNullByDefault() {
        Family family = new Family();

        assertNull(family.getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should override name with new value")
    void testOverwriteName() {
        Family family = new Family();
        family.setName("Vechi");
        family.setName("Nou");

        assertEquals("Nou", family.getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly set and get createdAt")
    void testCreatedAtGetterSetter() {
        Family family = new Family();
        LocalDate date = LocalDate.of(2023, 5, 20);
        family.setCreatedAt(date);

        assertEquals(date, family.getCreatedAt());
    }

    @Test
    @DisplayName("Model Unit Test: Should have null createdAt by default")
    void testCreatedAtIsNullByDefault() {
        Family family = new Family();

        assertNull(family.getCreatedAt());
    }

    @Test
    @DisplayName("Model Unit Test: Should allow setting createdAt to null")
    void testCreatedAtCanBeSetToNull() {
        Family family = new Family();
        family.setCreatedAt(LocalDate.now());
        family.setCreatedAt(null);

        assertNull(family.getCreatedAt());
    }
}