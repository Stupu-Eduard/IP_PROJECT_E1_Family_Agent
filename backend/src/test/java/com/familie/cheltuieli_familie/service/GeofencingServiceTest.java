package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.model.GeofenceZone;
import com.familie.cheltuieli_familie.repository.AlertRepository;
import com.familie.cheltuieli_familie.repository.GeofenceRepository;
import com.familie.cheltuieli_familie.security.service.GeofencingService;
import com.familie.cheltuieli_familie.service.FirebaseNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        MockitoAnnotations.openMocks(this);
        geometryFactory = new GeometryFactory();

        // Creăm un pătrat de test (0,0) până la (10,10)
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(0, 0), new Coordinate(0, 10),
                new Coordinate(10, 10), new Coordinate(10, 0),
                new Coordinate(0, 0) // Poligonul trebuie să se închidă
        };
        Polygon area = geometryFactory.createPolygon(coords);

        testZone = new GeofenceZone();
        testZone.setName("Zona de Siguranta Test");
        testZone.setArea(area);

        when(geofenceRepository.findAllByIsActiveTrue()).thenReturn(List.of(testZone));
    }

    @Test
    void processLocationUpdate_InsideZone_NoAlertTriggered() {
        // Punct în interiorul poligonului (5, 5)
        Point insidePoint = geometryFactory.createPoint(new Coordinate(5, 5));

        geofencingService.processLocationUpdate(1L, 2L, insidePoint);

        // Verificăm că NU s-a salvat nicio alertă, pentru că a respectat perimetrul
        verify(alertRepository, never()).save(any(Alert.class));
        verify(firebaseNotificationService, never()).sendPushNotification(anyString(), anyString(), anyString());
    }

    @Test
    void processLocationUpdate_OutsideZone_AlertTriggeredAndSaved() {
        // Punct în afara poligonului (15, 15)
        Point outsidePoint = geometryFactory.createPoint(new Coordinate(15, 15));

        geofencingService.processLocationUpdate(1L, 2L, outsidePoint);

        // Verificăm că a declanșat protocolul și a salvat alerta în DB
        verify(alertRepository, times(1)).save(any(Alert.class));
    }
}