package com.familie.cheltuieli_familie.dto.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentResponseDTOTest {

    @Test
    void textResponseDTO_shouldSetTypeAndMessage() {
        TextResponseDTO dto = new TextResponseDTO("Hello");
        assertEquals("text", dto.getType());
        assertEquals("Hello", dto.getMessage());
    }

    @Test
    void chartResponseDTO_shouldSetTypeMessageAndPayload() {
        ChartPayload payload = ChartPayload.builder()
                .chartType("bar")
                .title("Test")
                .build();
        ChartResponseDTO dto = new ChartResponseDTO("Chart data", payload);

        assertEquals("chart", dto.getType());
        assertEquals("Chart data", dto.getMessage());
        assertNotNull(dto.getPayload());
        assertEquals("bar", dto.getPayload().getChartType());
    }

    @Test
    void chartResponseDTO_shouldHaveNoArgsConstructor() {
        ChartResponseDTO dto = new ChartResponseDTO();
        assertNotNull(dto);
    }

    @Test
    void textResponseDTO_shouldHaveNoArgsConstructor() {
        TextResponseDTO dto = new TextResponseDTO();
        assertNotNull(dto);
    }

    @Test
    void agentResponseDTO_shouldHaveProtectedConstructor() {
        // The protected constructor is used by subclasses
        TextResponseDTO dto = new TextResponseDTO("test");
        assertEquals("text", dto.getType());
    }
}
