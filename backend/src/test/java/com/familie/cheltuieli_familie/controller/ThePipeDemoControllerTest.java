package com.familie.cheltuieli_familie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familie.cheltuieli_familie.service.ThePipeHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ThePipeDemoControllerTest {

    private MockMvc mockMvc;
    private ThePipeHandler thePipeHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        thePipeHandler = mock(ThePipeHandler.class);
        objectMapper = new ObjectMapper();
        ThePipeDemoController controller = new ThePipeDemoController(thePipeHandler, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void hello_TrimiteMesajSiReturneazaOk() throws Exception {
        mockMvc.perform(post("/api/v1/demo/pipe/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK - Mesaj de salut trimis"));

        verify(thePipeHandler, times(1)).broadcast(anyString());
    }

    @Test
    void simulateChildSafe_TrimiteLocatieSiReturneazaOk() throws Exception {
        mockMvc.perform(post("/api/v1/demo/pipe/child-safe"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK - Locatie sigura trimisa (Piata Universitatii)"));

        verify(thePipeHandler, times(1)).broadcast(anyString());
    }

    @Test
    void simulateChildDanger_TrimiteAlertaSiReturneazaOk() throws Exception {
        mockMvc.perform(post("/api/v1/demo/pipe/child-danger"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK - ALERTA trimisa: Copil in zona restrictionata!"));

        verify(thePipeHandler, times(1)).broadcast(anyString());
    }
}
