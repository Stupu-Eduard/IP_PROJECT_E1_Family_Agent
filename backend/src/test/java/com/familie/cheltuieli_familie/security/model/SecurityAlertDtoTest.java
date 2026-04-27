package com.familie.cheltuieli_familie.security.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAlertDtoTest {

    @Test
    void testDefaultConstructor() {
        SecurityAlertDto dto = new SecurityAlertDto();
        assertNull(dto.getChildId());
        assertNull(dto.getParentId());
        assertNull(dto.getAlertMessage());
        assertNull(dto.getRestrictedCategory());
        assertNull(dto.getTimestamp());
    }

    @Test
    void testAllArgsConstructor() {
        SecurityAlertDto dto = new SecurityAlertDto(1L, 2L, "Test message", "GEOFENCING", 12345678L);
        assertEquals(1L, dto.getChildId());
        assertEquals(2L, dto.getParentId());
        assertEquals("Test message", dto.getAlertMessage());
        assertEquals("GEOFENCING", dto.getRestrictedCategory());
        assertEquals(12345678L, dto.getTimestamp());
    }

    @Test
    void testSettersAndGetters() {
        SecurityAlertDto dto = new SecurityAlertDto();
        dto.setChildId(10L);
        dto.setParentId(20L);
        dto.setAlertMessage("Alert");
        dto.setRestrictedCategory("CATEGORY");
        dto.setTimestamp(999L);

        assertEquals(10L, dto.getChildId());
        assertEquals(20L, dto.getParentId());
        assertEquals("Alert", dto.getAlertMessage());
        assertEquals("CATEGORY", dto.getRestrictedCategory());
        assertEquals(999L, dto.getTimestamp());
    }

    @Test
    void testSerialization() {
        SecurityAlertDto dto = new SecurityAlertDto(1L, 2L, "msg", "cat", 100L);
        assertInstanceOf(java.io.Serializable.class, dto);
    }
}
