package com.familie.cheltuieli_familie.security.controller;

import com.familie.cheltuieli_familie.security.service.LocationStreamService;
import com.familie.cheltuieli_familie.security.service.LocationValidationService;
import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

class ChildLocationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LocationStreamService locationStreamService;
    @Mock
    private MinorSafetyFilterService minorSafetyFilterService;
    @Mock
    private LocationValidationService locationValidationService;

    @InjectMocks
    private ChildLocationController childLocationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Pornim doar Controller-ul, fără restul sistemului (fără DB, fără Security real)
        mockMvc = MockMvcBuilders.standaloneSetup(childLocationController).build();
    }

    @Test
    void testSyncLocationSuccess() throws Exception {
        Mockito.when(locationValidationService.isLocationValid(44.4268, 26.1025)).thenReturn(true);

        String json = "{\"childId\":1,\"parentId\":2,\"latitude\":44.4268,\"longitude\":26.1025}";

        mockMvc.perform(post("/api/v1/child/location/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("Locatie sincronizata cu succes."));
    }

    @Test
    void testSyncLocationInvalid() throws Exception {
        Mockito.when(locationValidationService.isLocationValid(0.0, 0.0)).thenReturn(false);

        String json = "{\"childId\":1,\"parentId\":2,\"latitude\":0.0,\"longitude\":0.0}";

        mockMvc.perform(post("/api/v1/child/location/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Eroare: Locatie GPS invalida (ex: 0,0). Sincronizare oprita."));
    }
}