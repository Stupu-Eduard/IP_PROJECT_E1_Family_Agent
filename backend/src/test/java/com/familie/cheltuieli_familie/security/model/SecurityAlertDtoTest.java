package com.familie.cheltuieli_familie.security.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityAlertDtoTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        // Apelăm constructorul gol (pentru a acoperi prima metodă)
        SecurityAlertDto dto = new SecurityAlertDto();

        // Verificăm că inițial valorile sunt null (opțional, dar recomandat)
        assertNull(dto.getChildId());

        // Apelăm toate metodele de tip "setter"
        dto.setChildId(1L);
        dto.setParentId(2L);
        dto.setAlertMessage("Alerta de test");
        dto.setRestrictedCategory("Jocuri video");
        dto.setTimestamp(1690000000L);

        // Apelăm toate metodele de tip "getter" și verificăm dacă datele au fost salvate corect
        assertEquals(1L, dto.getChildId());
        assertEquals(2L, dto.getParentId());
        assertEquals("Alerta de test", dto.getAlertMessage());
        assertEquals("Jocuri video", dto.getRestrictedCategory());
        assertEquals(1690000000L, dto.getTimestamp());
    }

    @Test
    void testAllArgsConstructor() {
        // Apelăm constructorul cu toți parametrii
        SecurityAlertDto dto = new SecurityAlertDto(10L, 20L, "Mesaj critic", "Dulciuri", 1700000000L);

        // Verificăm dacă valorile au fost alocate corect încă de la inițializare
        assertEquals(10L, dto.getChildId());
        assertEquals(20L, dto.getParentId());
        assertEquals("Mesaj critic", dto.getAlertMessage());
        assertEquals("Dulciuri", dto.getRestrictedCategory());
        assertEquals(1700000000L, dto.getTimestamp());
    }
}