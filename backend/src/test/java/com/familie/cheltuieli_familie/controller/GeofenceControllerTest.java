package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.security.service.GeofencingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeofenceControllerTest {

    private GeofencingService mockGeofencingService;
    private GeofenceController geofenceController;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @BeforeEach
    void setUp() {
        mockGeofencingService = mock(GeofencingService.class);
        geofenceController = new GeofenceController(mockGeofencingService);
    }

    @Test
    void testCheckUserLocation_Success() {
        Point validPoint = geometryFactory.createPoint(new Coordinate(26.1025, 44.4268));
        ResponseEntity<String> response = geofenceController.checkUserLocation(validPoint);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Locația a fost recepționată și procesată.", response.getBody());
        verify(mockGeofencingService, times(1)).isUserInsideZone(validPoint);
    }

    @Test
    void testCheckUserLocation_NullData() {
        ResponseEntity<String> response = geofenceController.checkUserLocation(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(mockGeofencingService, never()).isUserInsideZone(any());
    }

    @Test
    void testCheckUserLocation_InternalServerError() {
        Point validPoint = geometryFactory.createPoint(new Coordinate(26.1025, 44.4268));
        doThrow(new RuntimeException("Eroare simulata")).when(mockGeofencingService).isUserInsideZone(any(Point.class));

        ResponseEntity<String> response = geofenceController.checkUserLocation(validPoint);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("A apărut o eroare internă la procesarea coordonatelor.", response.getBody());
    }
}