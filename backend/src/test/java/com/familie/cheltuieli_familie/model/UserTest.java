package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Model Unit Test: Should have null id before persistence")
    void testIdIsNullByDefault() {
        User user = new User();

        assertNull(user.getId());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly set and get name")
    void testNameGetterSetter() {
        User user = new User();
        user.setName("Alexandra");

        assertEquals("Alexandra", user.getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should have null name by default")
    void testNameIsNullByDefault() {
        User user = new User();

        assertNull(user.getName());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly set and get email")
    void testEmailGetterSetter() {
        User user = new User();
        user.setEmail("alexandra@test.com");

        assertEquals("alexandra@test.com", user.getEmail());
    }

    @Test
    @DisplayName("Model Unit Test: Should have null email by default")
    void testEmailIsNullByDefault() {
        User user = new User();

        assertNull(user.getEmail());
    }

    @Test
    @DisplayName("Model Unit Test: Should override email with new value")
    void testOverwriteEmail() {
        User user = new User();
        user.setEmail("vechi@test.com");
        user.setEmail("nou@test.com");

        assertEquals("nou@test.com", user.getEmail());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly set and get passwordH")
    void testPasswordHGetterSetter() {
        User user = new User();
        user.setPasswordH("hashed_secret");

        assertEquals("hashed_secret", user.getPasswordH());
    }

    @Test
    @DisplayName("Model Unit Test: Should have null passwordH by default")
    void testPasswordHIsNullByDefault() {
        User user = new User();

        assertNull(user.getPasswordH());
    }

    @Test
    @DisplayName("Model Unit Test: Should correctly set and get createdAt")
    void testCreatedAtGetterSetter() {
        User user = new User();
        LocalDate date = LocalDate.of(2024, 6, 1);
        user.setCreatedAt(date);

        assertEquals(date, user.getCreatedAt());
    }

    @Test
    @DisplayName("Model Unit Test: Should have null createdAt by default")
    void testCreatedAtIsNullByDefault() {
        User user = new User();

        assertNull(user.getCreatedAt());
    }
}