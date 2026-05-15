package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.GeofenceZoneResponseDto;
import com.familie.cheltuieli_familie.dto.LatLngDto;
import com.familie.cheltuieli_familie.dto.SaveGeofenceZoneRequest;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.GeofenceZone;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.GeofenceRepository;
import com.familie.cheltuieli_familie.security.service.GeofencingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeofenceControllerTest {

    private GeofencingService mockGeofencingService;
    private GeofenceRepository mockGeofenceRepository;
    private FamilyMemberRepository mockFamilyMemberRepository;
    private GeofenceController geofenceController;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private static final Long USER_ID = 10L;
    private static final Long FAMILY_ID = 1L;

    @BeforeEach
    void setUp() {
        mockGeofencingService = mock(GeofencingService.class);
        mockGeofenceRepository = mock(GeofenceRepository.class);
        mockFamilyMemberRepository = mock(FamilyMemberRepository.class);
        geofenceController = new GeofenceController(mockGeofencingService, mockGeofenceRepository, mockFamilyMemberRepository);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Authentication buildAuth(Long userId) {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(mockUser);
        return auth;
    }

    private void stubFamilyId(Long userId, Long familyId) {
        if (familyId == null) {
            when(mockFamilyMemberRepository.findByUserId(userId)).thenReturn(List.of());
        } else {
            Family mockFamily = mock(Family.class);
            when(mockFamily.getId()).thenReturn(familyId);
            FamilyMember mockFm = mock(FamilyMember.class);
            when(mockFm.getFamily()).thenReturn(mockFamily);
            when(mockFamilyMemberRepository.findByUserId(userId)).thenReturn(List.of(mockFm));
        }
    }

    private Polygon buildTestPolygon() {
        Coordinate[] ring = {
                new Coordinate(27.5, 47.1),
                new Coordinate(27.7, 47.1),
                new Coordinate(27.7, 47.2),
                new Coordinate(27.5, 47.2),
                new Coordinate(27.5, 47.1)
        };
        return geometryFactory.createPolygon(ring);
    }

    // ── checkUserLocation ────────────────────────────────────────────────────

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

    // ── saveZone ─────────────────────────────────────────────────────────────

    @Test
    void saveZone_success_fara_zonaVeche() {
        Authentication auth = buildAuth(USER_ID);
        stubFamilyId(USER_ID, FAMILY_ID);
        when(mockGeofenceRepository.findByParentIdAndIsActiveTrue(FAMILY_ID)).thenReturn(Optional.empty());

        List<LatLngDto> coords = List.of(
                new LatLngDto(47.1, 27.5), new LatLngDto(47.2, 27.5),
                new LatLngDto(47.2, 27.7), new LatLngDto(47.1, 27.7)
        );
        SaveGeofenceZoneRequest request = new SaveGeofenceZoneRequest(coords, "Zona Test");

        ResponseEntity<Map<String, String>> response = geofenceController.saveZone(request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Zonă de siguranță salvată cu succes.", response.getBody().get("message"));
        verify(mockGeofenceRepository, times(1)).save(any(GeofenceZone.class));
    }

    @Test
    void saveZone_success_dezactiveazaZonaVeche() {
        Authentication auth = buildAuth(USER_ID);
        stubFamilyId(USER_ID, FAMILY_ID);

        GeofenceZone oldZone = GeofenceZone.builder()
                .id(5L).parentId(FAMILY_ID).area(buildTestPolygon()).isActive(true).build();
        when(mockGeofenceRepository.findByParentIdAndIsActiveTrue(FAMILY_ID)).thenReturn(Optional.of(oldZone));

        List<LatLngDto> coords = List.of(
                new LatLngDto(47.1, 27.5), new LatLngDto(47.2, 27.5),
                new LatLngDto(47.2, 27.7), new LatLngDto(47.1, 27.7)
        );
        SaveGeofenceZoneRequest request = new SaveGeofenceZoneRequest(coords, "Zona Noua");

        ResponseEntity<Map<String, String>> response = geofenceController.saveZone(request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(oldZone.isActive());
        verify(mockGeofenceRepository, times(2)).save(any(GeofenceZone.class));
    }

    @Test
    void saveZone_success_numeNull_folosesteFallback() {
        Authentication auth = buildAuth(USER_ID);
        stubFamilyId(USER_ID, FAMILY_ID);
        when(mockGeofenceRepository.findByParentIdAndIsActiveTrue(FAMILY_ID)).thenReturn(Optional.empty());

        List<LatLngDto> coords = List.of(
                new LatLngDto(47.1, 27.5), new LatLngDto(47.2, 27.5),
                new LatLngDto(47.2, 27.7), new LatLngDto(47.1, 27.7)
        );
        SaveGeofenceZoneRequest request = new SaveGeofenceZoneRequest(coords, null);

        ResponseEntity<Map<String, String>> response = geofenceController.saveZone(request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mockGeofenceRepository, times(1)).save(argThat(z -> "Zona de Siguranță".equals(z.getName())));
    }

    @Test
    void saveZone_fara_familie_returnsBadRequest() {
        Authentication auth = buildAuth(USER_ID);
        stubFamilyId(USER_ID, null);

        List<LatLngDto> coords = List.of(new LatLngDto(47.1, 27.5), new LatLngDto(47.2, 27.7));
        SaveGeofenceZoneRequest request = new SaveGeofenceZoneRequest(coords, "Test");

        ResponseEntity<Map<String, String>> response = geofenceController.saveZone(request, auth);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(mockGeofenceRepository, never()).save(any());
    }

    // ── getMyZone ─────────────────────────────────────────────────────────────

    @Test
    void getMyZone_zonaExista_returnsOk() {
        Authentication auth = buildAuth(USER_ID);
        stubFamilyId(USER_ID, FAMILY_ID);

        GeofenceZone zone = GeofenceZone.builder()
                .id(3L).parentId(FAMILY_ID).name("Zona Mea").area(buildTestPolygon()).isActive(true).build();
        when(mockGeofenceRepository.findByParentIdAndIsActiveTrue(FAMILY_ID)).thenReturn(Optional.of(zone));

        ResponseEntity<?> response = geofenceController.getMyZone(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        GeofenceZoneResponseDto body = (GeofenceZoneResponseDto) response.getBody();
        assertNotNull(body);
        assertEquals(3L, body.id());
        assertEquals("Zona Mea", body.name());
        assertFalse(body.coordinates().isEmpty());
    }

    @Test
    void getMyZone_nuExistaZona_returns204() {
        Authentication auth = buildAuth(USER_ID);
        stubFamilyId(USER_ID, FAMILY_ID);
        when(mockGeofenceRepository.findByParentIdAndIsActiveTrue(FAMILY_ID)).thenReturn(Optional.empty());

        ResponseEntity<?> response = geofenceController.getMyZone(auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getMyZone_fara_familie_returns204() {
        Authentication auth = buildAuth(USER_ID);
        stubFamilyId(USER_ID, null);

        ResponseEntity<?> response = geofenceController.getMyZone(auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(mockGeofenceRepository, never()).findByParentIdAndIsActiveTrue(any());
    }

    // ── deleteMyZone ──────────────────────────────────────────────────────────

    @Test
    void deleteMyZone_zonaExista_dezactiveazaSiReturnsOk() {
        Authentication auth = buildAuth(USER_ID);
        stubFamilyId(USER_ID, FAMILY_ID);

        GeofenceZone zone = GeofenceZone.builder()
                .id(4L).parentId(FAMILY_ID).area(buildTestPolygon()).isActive(true).build();
        when(mockGeofenceRepository.findByParentIdAndIsActiveTrue(FAMILY_ID)).thenReturn(Optional.of(zone));

        ResponseEntity<Map<String, String>> response = geofenceController.deleteMyZone(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(zone.isActive());
        verify(mockGeofenceRepository, times(1)).save(zone);
    }

    @Test
    void deleteMyZone_nuExistaZona_returnsOk() {
        Authentication auth = buildAuth(USER_ID);
        stubFamilyId(USER_ID, FAMILY_ID);
        when(mockGeofenceRepository.findByParentIdAndIsActiveTrue(FAMILY_ID)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, String>> response = geofenceController.deleteMyZone(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mockGeofenceRepository, never()).save(any());
    }

    @Test
    void deleteMyZone_fara_familie_returnsOk() {
        Authentication auth = buildAuth(USER_ID);
        stubFamilyId(USER_ID, null);

        ResponseEntity<Map<String, String>> response = geofenceController.deleteMyZone(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(mockGeofenceRepository, never()).findByParentIdAndIsActiveTrue(any());
    }
}
