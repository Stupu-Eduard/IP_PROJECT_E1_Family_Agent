package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.security.service.GeofencingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class GeofenceControllerTest {

    @Mock
    private GeofencingService geofencingService;

    @InjectMocks
    private GeofenceController geofenceController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void checkUserLocation_ValidData_ReturnsOk() {
        // 1. Arrange
        GeofenceController.LocationDto testLocation = new GeofenceController.LocationDto();
        // Folosim settere pentru a accesa datele, acum că sunt private
        testLocation.setLat(44.4268);
        testLocation.setLng(26.1025);

        // 2. Act
        ResponseEntity<String> response = geofenceController.checkUserLocation(testLocation);

        // 3. Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Locația a fost recepționată și procesată.", response.getBody());

        verify(geofencingService).processLocationUpdate(eq(1L), eq(2L), any(Point.class));
    }

    @Test
    void checkUserLocation_NullData_ReturnsBadRequest() {
        // 1. Act
        ResponseEntity<String> response = geofenceController.checkUserLocation(null);

        // 2. Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Eroare: Datele de locație lipsesc.", response.getBody());
    }
}