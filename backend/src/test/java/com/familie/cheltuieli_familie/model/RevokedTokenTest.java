package com.familie.cheltuieli_familie.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RevokedTokenTest {

    @Test
    @DisplayName("Test 1: Should have null jti before persistence")
    void testJtiIsNullByDefault() {
        RevokedToken revokedToken = new RevokedToken();

        assertNull(revokedToken.getJti());
    }

    @Test
    @DisplayName("Test 2 : Should have null revokedAt by default")
    void testRevokedAtIsNullByDefault() {
        RevokedToken revokedToken = new RevokedToken();

        assertNull(revokedToken.getRevokedAt());
    }

    @Test
    @DisplayName("Test 3: Should have null expiresAt by default")
    void testExpiresAtIsNullByDefault() {
        RevokedToken revokedToken = new RevokedToken();

        assertNull(revokedToken.getExpiresAt());
    }

    @Test
    @DisplayName("Test 4: Should correctly set and get jti through constructor")
    void testJtiGetterFromConstructor() {
        LocalDateTime revokedAt = LocalDateTime.parse("2026-05-10T10:00:00");
        LocalDateTime expiresAt = LocalDateTime.parse("2026-05-10T11:00:00");

        RevokedToken revokedToken = new RevokedToken(
                "token-jti-123",
                revokedAt,
                expiresAt
        );

        assertEquals("token-jti-123", revokedToken.getJti());
    }

    @Test
    @DisplayName("Test 5: Should correctly set and get revokedAt through constructor")
    void testRevokedAtGetterFromConstructor() {
        LocalDateTime revokedAt = LocalDateTime.parse("2026-05-10T10:00:00");
        LocalDateTime expiresAt = LocalDateTime.parse("2026-05-10T11:00:00");

        RevokedToken revokedToken = new RevokedToken(
                "token-jti-123",
                revokedAt,
                expiresAt
        );

        assertEquals(revokedAt, revokedToken.getRevokedAt());
    }

    @Test
    @DisplayName("Test 6: Should correctly set and get expiresAt through constructor")
    void testExpiresAtGetterFromConstructor() {
        LocalDateTime revokedAt = LocalDateTime.parse("2026-05-10T10:00:00");
        LocalDateTime expiresAt = LocalDateTime.parse("2026-05-10T11:00:00");

        RevokedToken revokedToken = new RevokedToken(
                "token-jti-123",
                revokedAt,
                expiresAt
        );

        assertEquals(expiresAt, revokedToken.getExpiresAt());
    }

    @Test
    @DisplayName("Test 7: Should allow null values through constructor")
    void testConstructorAllowsNullValues() {
        RevokedToken revokedToken = new RevokedToken(
                null,
                null,
                null
        );

        assertNull(revokedToken.getJti());
        assertNull(revokedToken.getRevokedAt());
        assertNull(revokedToken.getExpiresAt());
    }

    @Test
    @DisplayName("Test 8: Should be annotated as JPA entity")
    void testEntityAnnotationExists() {
        assertTrue(RevokedToken.class.isAnnotationPresent(Entity.class));
    }

    @Test
    @DisplayName("Test 9: Should be mapped to revoked_tokens table")
    void testTableAnnotation() {
        Table table = RevokedToken.class.getAnnotation(Table.class);

        assertNotNull(table);
        assertEquals("revoked_tokens", table.name());
    }

    @Test
    @DisplayName("Test 10: Should have jti as primary key")
    void testJtiHasIdAnnotation() throws NoSuchFieldException {
        Field field = RevokedToken.class.getDeclaredField("jti");

        assertTrue(field.isAnnotationPresent(Id.class));
    }

    @Test
    @DisplayName("Test 11: Should correctly map jti column")
    void testJtiColumnAnnotation() throws NoSuchFieldException {
        Field field = RevokedToken.class.getDeclaredField("jti");
        Column column = field.getAnnotation(Column.class);

        assertNotNull(column);
        assertEquals("jti", column.name());
        assertFalse(column.nullable());
        assertEquals(255, column.length());
    }

    @Test
    @DisplayName("Test 12: Should correctly map revokedAt column")
    void testRevokedAtColumnAnnotation() throws NoSuchFieldException {
        Field field = RevokedToken.class.getDeclaredField("revokedAt");
        Column column = field.getAnnotation(Column.class);

        assertNotNull(column);
        assertEquals("revoked_at", column.name());
        assertFalse(column.nullable());
    }

    @Test
    @DisplayName("Test 13: Should correctly map expiresAt column")
    void testExpiresAtColumnAnnotation() throws NoSuchFieldException {
        Field field = RevokedToken.class.getDeclaredField("expiresAt");
        Column column = field.getAnnotation(Column.class);

        assertNotNull(column);
        assertEquals("expires_at", column.name());
        assertFalse(column.nullable());
    }
}
