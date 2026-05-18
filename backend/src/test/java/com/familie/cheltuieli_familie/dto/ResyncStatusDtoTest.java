package com.familie.cheltuieli_familie.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResyncStatusDtoTest {

    @Test
    void testResyncStatusDtoInstantiationAndFieldAccess() {
        ResyncStatusDto dto = new ResyncStatusDto(42, 3);

        assertEquals(42, dto.processedCount());
        assertEquals(3, dto.errorCount());
    }
}
