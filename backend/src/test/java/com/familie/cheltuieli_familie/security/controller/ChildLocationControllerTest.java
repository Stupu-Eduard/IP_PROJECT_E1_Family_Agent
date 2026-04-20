package com.familie.cheltuieli_familie.security.controller;

import com.familie.cheltuieli_familie.security.service.LocationStreamService;
import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChildLocationController.class)
@AutoConfigureMockMvc(addFilters = false) // Dezactivam securitatea ca sa nu ne incurce la test
class ChildLocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationStreamService locationStreamService;

    @MockitoBean
    private MinorSafetyFilterService minorSafetyFilterService;

    @Test
    void testUpdateLocation() throws Exception {
        String json = "{\"childId\":1, \"parentId\":2, \"latitude\":44.4, \"longitude\":26.1, \"placeTypes\":[\"park\"]}";

        mockMvc.perform(post("/api/security/location/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(locationStreamService).sendLocationToParent(2L, 44.4, 26.1);
        verify(minorSafetyFilterService).evaluateChildLocation(1L, 2L, List.of("park"));
    }
}