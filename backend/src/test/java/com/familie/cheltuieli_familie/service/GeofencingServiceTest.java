package com.familie.cheltuieli_familie.service;
import com.familie.cheltuieli_familie.security.service.GeofencingService;
import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.model.GeofenceZone;
import com.familie.cheltuieli_familie.repository.AlertRepository;
import com.familie.cheltuieli_familie.repository.GeofenceRepository;
import com.familie.cheltuieli_familie.service.FirebaseNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeofencingServiceTest {

    @Mock
    private FirebaseNotificationService firebaseNotificationService;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private GeofenceRepository geofenceRepository;

    @InjectMocks
    private GeofencingService geofencingService;

    private GeometryFactory geometryFactory;
    private GeofenceZone testZone;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory();

        // Injectăm token-ul de Firebase folosind Reflection (deoarece e un @Value)
        ReflectionTestUtils.setField(geofencingService, "parentDeviceToken", "test-token-123");

        // Creăm un poligon de test (un pătrat de la coordonatele 0,0 până la 10,10)
        Coordinate[] coordinates = new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(10, 0),
                new Coordinate(10, 10),
                new Coordinate(0, 10),
                new Coordinate(0, 0) // Poligonul trebuie să se închidă în același punct
        };
        Polygon testPolygon = geometryFactory.createPolygon(coordinates);

        testZone = new GeofenceZone();
        testZone.setId(1L);
        testZone.setName("Școala Generală");
        testZone.setArea(testPolygon);
        testZone.setActive(true);
    }

    @Test
    void testProcessLocationUpdate_WhenUserIsInside_ShouldNotAlert() {
        // ARANJARE
        when(geofenceRepository.findAllByIsActiveTrue()).thenReturn(List.of(testZone));

        // Creăm un punct aflat în interiorul pătratului (ex: x=5, y=5)
        Point insidePoint = geometryFactory.createPoint(new Coordinate(5, 5));

        // ACȚIUNE
        geofencingService.processLocationUpdate(100L, 200L, insidePoint);

        // VERIFICARE
        // Ne asigurăm că NU s-a salvat nimic în baza de date și NU s-a trimis nicio notificare
        verify(alertRepository, never()).save(any(Alert.class));
        verify(firebaseNotificationService, never()).sendPushNotification(anyString(), anyString(), anyString());
    }

    @Test
    void testProcessLocationUpdate_WhenUserIsOutside_ShouldTriggerAuditAndAlert() {
        // ARANJARE
        when(geofenceRepository.findAllByIsActiveTrue()).thenReturn(List.of(testZone));

        // Creăm un punct aflat clar în afara pătratului (ex: x=15, y=15)
        Point outsidePoint = geometryFactory.createPoint(new Coordinate(15, 15));

        // ACȚIUNE
        geofencingService.processLocationUpdate(100L, 200L, outsidePoint);

        // VERIFICARE 1: S-a apelat baza de date pentru audit?
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertCaptor.capture());

        Alert savedAlert = alertCaptor.getValue();
        assertEquals(100L, savedAlert.getChildId());
        assertEquals(200L, savedAlert.getParentId());
        assertEquals("GEOFENCE_VIOLATION", savedAlert.getRestrictedCategory());
        assertTrue(savedAlert.getMessage().contains("Școala Generală"));

        // VERIFICARE 2: S-a trimis notificarea către Firebase?
        verify(firebaseNotificationService, times(1))
                .sendPushNotification(eq("test-token-123"), eq("Alertă Securitate"), anyString());
    }

    @Test
    void testProcessLocationUpdate_WithNullLocation_ShouldDoNothing() {
        // ACȚIUNE
        geofencingService.processLocationUpdate(100L, 200L, null);

        // VERIFICARE
        verify(geofenceRepository, never()).findAllByIsActiveTrue();
        verify(alertRepository, never()).save(any());
        verify(firebaseNotificationService, never()).sendPushNotification(any(), any(), any());
    }
}