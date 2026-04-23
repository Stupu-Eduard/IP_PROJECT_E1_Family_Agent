package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.security.service.GeofencingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GeofenceControllerTest {

    private GeofencingService geofencingService;
    private GeofenceController geofenceController;

    @BeforeEach
    void setUp() {
        // Mock-uim serviciul pentru a nu rula logica reală de geofencing la fiecare test
        geofencingService = mock(GeofencingService.class);
        geofenceController = new GeofenceController(geofencingService);
    }

    @Test
    void testCheckUserLocation_NullData_ReturnsBadRequest() {
        // SCENARIUL 1: Trimitem null pentru a forța intrarea pe primul IF
        ResponseEntity<String> response = geofenceController.checkUserLocation(null);

        // Verificăm dacă primim eroarea corectă
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Date invalide: Locatia nu poate fi nula.", response.getBody());

        // Verificăm că motorul de geofencing NU a fost apelat dacă datele au fost nule
        verifyNoInteractions(geofencingService);
    }

    @Test
    void testCheckUserLocation_ValidData_ReturnsOk() {
        // SCENARIUL 2: Trimitem date valide (simulăm un obiect)
        Object mockLocation = new Object();

        ResponseEntity<String> response = geofenceController.checkUserLocation(mockLocation);

        // Verificăm dacă a trecut cu succes prin "try"
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Locatie receptionata si procesata.", response.getBody());

        // Ne asigurăm că a apelat o singură dată metoda din service
        verify(geofencingService, times(1)).isUserInsideZone(mockLocation);
    }

    @Test
    void testCheckUserLocation_ServiceThrowsException_ReturnsInternalServerError() {
        // SCENARIUL 3: Simulăm că serverul/baza de date pică ca să forțăm intrarea pe CATCH
        Object mockLocation = new Object();

        // Învățăm mock-ul să arunce o eroare intenționat
        doThrow(new RuntimeException("Simulare eroare sistem"))
                .when(geofencingService).isUserInsideZone(mockLocation);

        ResponseEntity<String> response = geofenceController.checkUserLocation(mockLocation);

        // Verificăm că a prins eroarea corect și a returnat codul 500
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("A apărut o eroare internă la procesare.", response.getBody());

        // Verificăm că a încercat să o apeleze înainte să pice
        verify(geofencingService, times(1)).isUserInsideZone(mockLocation);
    }
}