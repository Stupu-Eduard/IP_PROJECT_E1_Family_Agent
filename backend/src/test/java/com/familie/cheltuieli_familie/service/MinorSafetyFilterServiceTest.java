package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.security.service.AlertService;
import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import com.familie.cheltuieli_familie.security.model.SecurityAlertDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Mockito = librarie care "simuleaza" dependentele (AlertService) fara baza de date reala
@ExtendWith(MockitoExtension.class)
class MinorSafetyFilterServiceTest {

    // Simulam AlertService - nu vrem sa trimitem alerte reale in teste
    @Mock
    private AlertService alertService;

    // Serviciul pe care il testam, cu AlertService injectat automat
    @InjectMocks
    private MinorSafetyFilterService minorSafetyFilterService;

    private static final Long CHILD_ID = 1L;
    private static final Long PARENT_ID = 2L;

    // =====================================================================
    // TESTE PENTRU isLocationRestricted()
    // =====================================================================

    @Test
    void isLocationRestricted_returneazaTrue_cand_loculEsteBar() {
        List<String> placeTypes = List.of("bar", "restaurant");
        assertTrue(minorSafetyFilterService.isLocationRestricted(placeTypes));
    }

    @Test
    void isLocationRestricted_returneazaTrue_cand_loculEsteNightClub() {
        List<String> placeTypes = List.of("night_club");
        assertTrue(minorSafetyFilterService.isLocationRestricted(placeTypes));
    }

    @Test
    void isLocationRestricted_returneazaFalse_cand_loculEsteRestaurant() {
        List<String> placeTypes = List.of("restaurant", "food");
        assertFalse(minorSafetyFilterService.isLocationRestricted(placeTypes));
    }

    @Test
    void isLocationRestricted_returneazaFalse_cand_listaEsteGoala() {
        assertFalse(minorSafetyFilterService.isLocationRestricted(List.of()));
    }

    @Test
    void isLocationRestricted_returneazaFalse_cand_listaEsteNull() {
        assertFalse(minorSafetyFilterService.isLocationRestricted(null));
    }

    @Test
    void isLocationRestricted_esteCase_insensitive() {
        // Google Maps poate returna "BAR" sau "Bar" - trebuie sa functioneze oricum
        List<String> placeTypes = List.of("BAR");
        assertTrue(minorSafetyFilterService.isLocationRestricted(placeTypes));
    }

    // =====================================================================
    // TESTE PENTRU evaluateChildLocation()
    // =====================================================================

    @Test
    void evaluateChildLocation_trimiteAlerta_cand_loculEsteRestricționat() {
        List<String> placeTypes = List.of("bar");

        minorSafetyFilterService.evaluateChildLocation(CHILD_ID, PARENT_ID, placeTypes);

        // Verificam ca AlertService a fost apelat exact o data
        verify(alertService, times(1)).sendPushNotificationToParent(any(SecurityAlertDto.class));
    }

    @Test
    void evaluateChildLocation_nuTrimiteAlerta_cand_loculEsteSignur() {
        List<String> placeTypes = List.of("restaurant", "cafe");

        minorSafetyFilterService.evaluateChildLocation(CHILD_ID, PARENT_ID, placeTypes);

        // Verificam ca AlertService NU a fost apelat
        verify(alertService, never()).sendPushNotificationToParent(any());
    }

    @Test
    void evaluateChildLocation_nuTrimiteAlerta_cand_listaEsteGoala() {
        minorSafetyFilterService.evaluateChildLocation(CHILD_ID, PARENT_ID, List.of());

        verify(alertService, never()).sendPushNotificationToParent(any());
    }

    @Test
    void evaluateChildLocation_nuTrimiteAlerta_cand_listaEsteNull() {
        minorSafetyFilterService.evaluateChildLocation(CHILD_ID, PARENT_ID, null);

        verify(alertService, never()).sendPushNotificationToParent(any());
    }

    @Test
    void evaluateChildLocation_alertaContineCategoriaCoreecta() {
        List<String> placeTypes = List.of("casino");

        minorSafetyFilterService.evaluateChildLocation(CHILD_ID, PARENT_ID, placeTypes);

        // Capturem alerta trimisa ca sa ii verificam continutul
        ArgumentCaptor<SecurityAlertDto> captor = ArgumentCaptor.forClass(SecurityAlertDto.class);
        verify(alertService).sendPushNotificationToParent(captor.capture());

        SecurityAlertDto alertTrimisa = captor.getValue();
        assertEquals(CHILD_ID, alertTrimisa.getChildId());
        assertEquals(PARENT_ID, alertTrimisa.getParentId());
        assertEquals("casino", alertTrimisa.getRestrictedCategory());
        assertNotNull(alertTrimisa.getTimestamp());
    }

    @Test
    void evaluateChildLocation_detecteazaPrimaCategoriRestrictionate_dintreMaiMulte() {
        // Daca lista contine mai multe categorii interzise, o detecteaza pe prima
        List<String> placeTypes = List.of("restaurant", "bar", "casino");

        minorSafetyFilterService.evaluateChildLocation(CHILD_ID, PARENT_ID, placeTypes);

        ArgumentCaptor<SecurityAlertDto> captor = ArgumentCaptor.forClass(SecurityAlertDto.class);
        verify(alertService).sendPushNotificationToParent(captor.capture());

        // "bar" apare primul in lista
        assertEquals("bar", captor.getValue().getRestrictedCategory());
    }
}