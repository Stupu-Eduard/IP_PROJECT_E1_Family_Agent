package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.model.GeofenceZone;
import com.familie.cheltuieli_familie.repository.AlertRepository;
import com.familie.cheltuieli_familie.repository.GeofenceRepository;
import com.familie.cheltuieli_familie.security.service.GeofencingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GeofencingServiceTest {

    private final GeometryFactory factory = new GeometryFactory();

    // Declarăm Mocks pentru toate cele 3 dependențe noi
    private FirebaseNotificationService mockFirebaseService;
    private AlertRepository mockAlertRepository;
    private GeofenceRepository mockGeofenceRepository;

    private GeofencingService service;

    @BeforeEach
    void setUp() throws Exception {
        // Inițializăm clonele (Mocks)
        mockFirebaseService = mock(FirebaseNotificationService.class);
        mockAlertRepository = mock(AlertRepository.class);
        mockGeofenceRepository = mock(GeofenceRepository.class);

        // Folosim constructorul corect, cu toți cei 3 parametri!
        service = new GeofencingService(mockFirebaseService, mockAlertRepository, mockGeofenceRepository);

        // Setăm token-ul de Firebase prin reflexie
        java.lang.reflect.Field tokenField = GeofencingService.class.getDeclaredField("parentDeviceToken");
        tokenField.setAccessible(true);
        tokenField.set(service, "test-token");
    }

    // O metodă ajutătoare pentru a nu scrie poligonul de 3 ori în teste
    private GeofenceZone createTestZone() {
        Coordinate[] coords = new Coordinate[] {
                new Coordinate(0,0),
                new Coordinate(0,10),
                new Coordinate(10,10),
                new Coordinate(10,0),
                new Coordinate(0,0) // Închidem pătratul
        };
        Polygon zoneSquare = factory.createPolygon(coords);

        GeofenceZone zone = new GeofenceZone();
        zone.setArea(zoneSquare);
        zone.setName("Zona Acasă");
        return zone;
    }

    @Test
    void testProcessLocationUpdate_CandEsteInInterior_NuDeclanseazaAlerta() {
        // Pregătim datele: simulăm că baza de date returnează zona noastră validă
        GeofenceZone zone = createTestZone();
        when(mockGeofenceRepository.findAllByIsActiveTrue()).thenReturn(Collections.singletonList(zone));

        // Punct în interiorul pătratului (5,5)
        Point insidePoint = factory.createPoint(new Coordinate(5,5));

        // Apelăm metoda principală
        service.processLocationUpdate(1L, 2L, insidePoint);

        // VERIFICARE: Fiind în interior, NU trebuie să se salveze nimic în baza de date și NU se trimite push
        verify(mockAlertRepository, never()).save(any(Alert.class));
        verify(mockFirebaseService, never()).sendPushNotification(anyString(), anyString(), anyString());
    }

    @Test
    void testProcessLocationUpdate_CandEsteInExterior_DeclanseazaAlerta() {
        // Pregătim datele
        GeofenceZone zone = createTestZone();
        when(mockGeofenceRepository.findAllByIsActiveTrue()).thenReturn(Collections.singletonList(zone));

        // Punct complet în afara pătratului (15,15)
        Point outsidePoint = factory.createPoint(new Coordinate(15,15));

        // Apelăm metoda principală
        service.processLocationUpdate(1L, 2L, outsidePoint);

        // VERIFICARE: Fiind în exterior, TREBUIE să salveze alerta în BD și TREBUIE să trimită notificare!
        verify(mockAlertRepository, times(1)).save(any(Alert.class));
        verify(mockFirebaseService, times(1)).sendPushNotification(anyString(), anyString(), anyString());
    }

    @Test
    void testProcessLocationUpdate_CandLocatiaEsteNull_OpresteExecutia() {
        // Apelăm metoda cu o locație null
        service.processLocationUpdate(1L, 2L, null);

        // VERIFICARE: Sistemul trebuie să se oprească din prima linie, deci baza de date nu este interogată
        verify(mockGeofenceRepository, never()).findAllByIsActiveTrue();
    }

    @Test
    void testProcessLocationUpdate_CandZonaNuArePoligonSetat_DeclanseazaAlerta() {
        // Simulăm o zonă coruptă din BD (care nu are coordonate)
        GeofenceZone emptyZone = new GeofenceZone();
        emptyZone.setName("Zona Corupta");
        when(mockGeofenceRepository.findAllByIsActiveTrue()).thenReturn(Collections.singletonList(emptyZone));

        Point validPoint = factory.createPoint(new Coordinate(5,5));

        service.processLocationUpdate(1L, 2L, validPoint);

        // În logica curentă, dacă zona e invalidă (area == null), algoritmul PIP va returna false
        // Prin urmare, sistemul va declanșa protocolul de eroare/violare
        verify(mockAlertRepository, times(1)).save(any(Alert.class));
    }
}