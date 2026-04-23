package com.familie.cheltuieli_familie.security.controller;

import com.familie.cheltuieli_familie.security.service.LocationStreamService;
import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Folosim MockitoBean pentru Spring Boot 3.4+
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
        // Trebuie sa folosim URL-ul EXACT din @RequestMapping + @PostMapping
        // Si cheile JSON identice cu campurile din record-ul LocationSyncRequest
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

        // Verificam ca serviciile au fost apelate cu datele din JSON
        verify(locationStreamService).sendLocationToParent(1L, 47.1585, 27.6014);
        verify(minorSafetyFilterService).evaluateChildLocation(2L, 1L, List.of("bar", "restaurant"));
    }
}