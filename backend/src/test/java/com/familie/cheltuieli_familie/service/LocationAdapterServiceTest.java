package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.LocationMapDto;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.GeofenceRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationAdapterServiceTest {

    @Mock
    private MinorSafetyFilterService minorSafetyFilterService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeofenceRepository geofenceRepository;

    @InjectMocks
    private LocationAdapterService locationAdapterService;

    private static final Long CHILD_ID = 1L;
    private static final Long PARENT_ID = 2L;
    private static final double LATITUDE = 47.1585;
    private static final double LONGITUDE = 27.6014;

    @BeforeEach
    void setUp() {
        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("Copil Test");
        when(userRepository.findById(CHILD_ID)).thenReturn(Optional.of(mockUser));
        when(geofenceRepository.findByParentIdAndIsActiveTrue(PARENT_ID)).thenReturn(Optional.empty());
    }

    // =====================================================================
    // TESTE PENTRU adapt()
    // =====================================================================

    @Test
    void adapt_returneazaLatSiLngCorecte() {
        List<String> placeTypes = List.of("restaurant");
        when(minorSafetyFilterService.isLocationRestricted(placeTypes)).thenReturn(false);

        LocationMapDto dto = locationAdapterService.adapt(CHILD_ID, PARENT_ID, LATITUDE, LONGITUDE, placeTypes);

        // Verificam ca lat si lng sunt corecte pentru Google Maps SDK
        assertEquals(LATITUDE, dto.lat());
        assertEquals(LONGITUDE, dto.lng());
    }

    @Test
    void adapt_returneazaChildIdSiParentIdCorecte() {
        List<String> placeTypes = List.of("restaurant");
        when(minorSafetyFilterService.isLocationRestricted(placeTypes)).thenReturn(false);

        LocationMapDto dto = locationAdapterService.adapt(CHILD_ID, PARENT_ID, LATITUDE, LONGITUDE, placeTypes);

        assertEquals(CHILD_ID, dto.childId());
        assertEquals(PARENT_ID, dto.parentId());
    }

    @Test
    void adapt_isRestricted_esteTrue_cand_loculEsteRestricționat() {
        List<String> placeTypes = List.of("bar");
        when(minorSafetyFilterService.isLocationRestricted(placeTypes)).thenReturn(true);

        LocationMapDto dto = locationAdapterService.adapt(CHILD_ID, PARENT_ID, LATITUDE, LONGITUDE, placeTypes);

        assertTrue(dto.isRestricted());
    }

    @Test
    void adapt_isRestricted_esteFalse_cand_loculEsteSignur() {
        List<String> placeTypes = List.of("restaurant", "cafe");
        when(minorSafetyFilterService.isLocationRestricted(placeTypes)).thenReturn(false);

        LocationMapDto dto = locationAdapterService.adapt(CHILD_ID, PARENT_ID, LATITUDE, LONGITUDE, placeTypes);

        assertFalse(dto.isRestricted());
    }

    @Test
    void adapt_timestamp_nuEsteNull() {
        List<String> placeTypes = List.of("restaurant");
        when(minorSafetyFilterService.isLocationRestricted(placeTypes)).thenReturn(false);

        LocationMapDto dto = locationAdapterService.adapt(CHILD_ID, PARENT_ID, LATITUDE, LONGITUDE, placeTypes);

        // Verificam ca timestamp-ul e setat automat
        assertNotNull(dto.timestamp());
    }

    @Test
    void adapt_apeleazaIsLocationRestricted_cuPlaceTypesCorecte() {
        List<String> placeTypes = List.of("casino");
        when(minorSafetyFilterService.isLocationRestricted(placeTypes)).thenReturn(true);

        locationAdapterService.adapt(CHILD_ID, PARENT_ID, LATITUDE, LONGITUDE, placeTypes);

        verify(minorSafetyFilterService, times(1)).isLocationRestricted(placeTypes);
    }

    @Test
    void adapt_isOutsideGeofence_esteFalse_cand_nuExistaZona() {
        List<String> placeTypes = List.of("restaurant");
        when(minorSafetyFilterService.isLocationRestricted(placeTypes)).thenReturn(false);
        when(geofenceRepository.findByParentIdAndIsActiveTrue(PARENT_ID)).thenReturn(Optional.empty());

        LocationMapDto dto = locationAdapterService.adapt(CHILD_ID, PARENT_ID, LATITUDE, LONGITUDE, placeTypes);

        assertFalse(dto.isOutsideGeofence());
    }
}