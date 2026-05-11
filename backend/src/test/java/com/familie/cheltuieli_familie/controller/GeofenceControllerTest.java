package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.GeofenceRequest;
import com.familie.cheltuieli_familie.model.GeofenceZone;
import com.familie.cheltuieli_familie.security.service.GeofencingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GeofenceController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class GeofenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeofencingService geofencingService;

    @MockitoBean
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSaveZone() throws Exception {
        GeofenceRequest request = new GeofenceRequest();
        request.setName("Test Zone");
        request.setPoints(List.of(new GeofenceRequest.PointDto(44.0, 26.0)));

        mockMvc.perform(post("/api/geofencing/save-zone")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Zona de siguranta a fost salvata cu succes."));
    }

    @Test
    void testSaveZone_Invalid() throws Exception {
        GeofenceRequest request = new GeofenceRequest();
        request.setPoints(List.of());

        mockMvc.perform(post("/api/geofencing/save-zone")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllZones() throws Exception {
        GeometryFactory gf = new GeometryFactory();
        GeofenceZone zone = new GeofenceZone();
        zone.setId(1L);
        zone.setName("Zone 1");
        zone.setArea(gf.createPolygon(new Coordinate[]{
                new Coordinate(26.0, 44.0),
                new Coordinate(26.1, 44.0),
                new Coordinate(26.1, 44.1),
                new Coordinate(26.0, 44.1),
                new Coordinate(26.0, 44.0)
        }));

        when(geofencingService.getAllActiveGeofences()).thenReturn(List.of(zone));

        mockMvc.perform(get("/api/geofencing/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Zone 1"));
    }

    @Test
    void testDeleteZone() throws Exception {
        when(geofencingService.deleteGeofence(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/geofencing/zones/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Zona a fost stearsa cu succes."));
    }

    @Test
    void testDeleteZone_NotFound() throws Exception {
        when(geofencingService.deleteGeofence(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/geofencing/zones/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteAllZones() throws Exception {
        doNothing().when(geofencingService).deleteAllGeofences();

        mockMvc.perform(delete("/api/geofencing/zones/all"))
                .andExpect(status().isOk())
                .andExpect(content().string("Toate zonele au fost sterse cu succes."));
    }

    @Test
    void testCheckUserLocation() throws Exception {
        GeofenceController.LocationDto dto = new GeofenceController.LocationDto();
        dto.setLat(44.0);
        dto.setLng(26.0);

        mockMvc.perform(post("/api/geofencing/check-location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}