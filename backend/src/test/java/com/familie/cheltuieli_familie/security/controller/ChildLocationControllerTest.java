package com.familie.cheltuieli_familie.security.controller;

import com.familie.cheltuieli_familie.security.service.LocationStreamService;
import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChildLocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChildLocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationStreamService locationStreamService;

    @MockitoBean
    private MinorSafetyFilterService minorSafetyFilterService;

    @Test
    void testSyncLocation() throws Exception {
        String json = """
                {
                  "childId": 2,
                  "parentId": 1,
                  "latitude": 47.1585,
                  "longitude": 27.6014,
                  "placeTypes": ["bar", "restaurant"]
                }
                """;

        mockMvc.perform(post("/api/v1/child/location/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // sendLocationToParent primeste acum 5 parametri dupa actualizare:
        // childId, parentId, latitude, longitude, placeTypes
        verify(locationStreamService).sendLocationToParent(
                2L, 1L, 47.1585, 27.6014, List.of("bar", "restaurant")
        );

        verify(minorSafetyFilterService).evaluateChildLocation(
                2L, 1L, List.of("bar", "restaurant")
        );
    }
}