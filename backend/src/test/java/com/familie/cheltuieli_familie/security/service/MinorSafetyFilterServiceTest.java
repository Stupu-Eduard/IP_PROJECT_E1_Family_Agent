package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.security.model.SecurityAlertDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MinorSafetyFilterServiceTest {

    private AlertService alertService;
    private MinorSafetyFilterService minorSafetyFilterService;

    @BeforeEach
    void setUp() {
        alertService = mock(AlertService.class);
        minorSafetyFilterService = new MinorSafetyFilterService(alertService);
    }

    @Test
    void evaluateChildLocation_nullPlaceTypes_doesNothing() {
        minorSafetyFilterService.evaluateChildLocation(1L, 2L, null);
        verifyNoInteractions(alertService);
    }

    @Test
    void evaluateChildLocation_emptyPlaceTypes_doesNothing() {
        minorSafetyFilterService.evaluateChildLocation(1L, 2L, List.of());
        verifyNoInteractions(alertService);
    }

    @Test
    void evaluateChildLocation_noRestrictedCategory_doesNotSendAlert() {
        minorSafetyFilterService.evaluateChildLocation(1L, 2L, List.of("restaurant", "park", "school"));
        verifyNoInteractions(alertService);
    }

    @Test
    void evaluateChildLocation_restrictedCategory_sendsAlertWithExpectedData() {
        Long childId = 10L;
        Long parentId = 20L;

        minorSafetyFilterService.evaluateChildLocation(childId, parentId, List.of("park", "casino", "mall"));

        ArgumentCaptor<SecurityAlertDto> captor = ArgumentCaptor.forClass(SecurityAlertDto.class);
        verify(alertService, times(1)).sendPushNotificationToParent(captor.capture());

        SecurityAlertDto alert = captor.getValue();
        assertNotNull(alert);
        assertEquals(childId, alert.getChildId());
        assertEquals(parentId, alert.getParentId());
        assertEquals("casino", alert.getRestrictedCategory());
        assertNotNull(alert.getAlertMessage());
        assertFalse(alert.getAlertMessage().isBlank());
        assertNotNull(alert.getTimestamp());
    }

    @Test
    void evaluateChildLocation_restrictedCategory_caseInsensitive_sendsAlert() {
        minorSafetyFilterService.evaluateChildLocation(1L, 2L, List.of("Park", "VAPE_SHOP"));

        ArgumentCaptor<SecurityAlertDto> captor = ArgumentCaptor.forClass(SecurityAlertDto.class);
        verify(alertService, times(1)).sendPushNotificationToParent(captor.capture());

        SecurityAlertDto alert = captor.getValue();
        assertNotNull(alert);
        assertEquals("vape_shop", alert.getRestrictedCategory());
        assertNotNull(alert.getTimestamp());
    }

    @Test
    void isLocationRestricted_null_returnsFalse() {
        assertFalse(minorSafetyFilterService.isLocationRestricted(null));
    }

    @Test
    void isLocationRestricted_empty_returnsFalse() {
        assertFalse(minorSafetyFilterService.isLocationRestricted(List.of()));
    }

    @Test
    void isLocationRestricted_withoutRestricted_returnsFalse() {
        assertFalse(minorSafetyFilterService.isLocationRestricted(List.of("park", "school")));
    }

    @Test
    void isLocationRestricted_withRestricted_returnsTrue() {
        assertTrue(minorSafetyFilterService.isLocationRestricted(List.of("school", "night_club")));
    }

    @Test
    void isLocationRestricted_caseInsensitive_returnsTrue() {
        assertTrue(minorSafetyFilterService.isLocationRestricted(List.of("LiQuOr_StOrE")));
    }
}