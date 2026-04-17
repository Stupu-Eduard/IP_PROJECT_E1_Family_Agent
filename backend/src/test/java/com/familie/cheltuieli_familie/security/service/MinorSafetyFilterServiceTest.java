package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.security.model.SecurityAlertDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    void evaluateChildLocation_restrictedCategory_sendsAlert() {
        Long childId = 10L;
        Long parentId = 20L;

        minorSafetyFilterService.evaluateChildLocation(childId, parentId, List.of("park", "casino", "mall"));

        ArgumentCaptor<SecurityAlertDto> captor = ArgumentCaptor.forClass(SecurityAlertDto.class);
        verify(alertService, times(1)).sendPushNotificationToParent(captor.capture());

        SecurityAlertDto alert = captor.getValue();
        assertNotNull(alert);

        Object dtoChildId = readProperty(alert, "getChildId", "childId", "child_id");
        Object dtoParentId = readProperty(alert, "getParentId", "parentId", "parent_id");
        Object dtoTriggeredCategory = readProperty(alert, "getTriggeredCategory", "triggeredCategory", "category", "type");
        Object dtoMessage = readProperty(alert, "getMessage", "message");

        assertEquals(childId, dtoChildId);
        assertEquals(parentId, dtoParentId);
        assertNotNull(dtoTriggeredCategory);
        assertEquals("casino", String.valueOf(dtoTriggeredCategory).toLowerCase());
        assertNotNull(dtoMessage);
        assertTrue(String.valueOf(dtoMessage).toLowerCase().contains("zona"));
    }

    @Test
    void evaluateChildLocation_restrictedCategory_caseInsensitive_sendsAlertLowercase() {
        minorSafetyFilterService.evaluateChildLocation(1L, 2L, List.of("Park", "VAPE_SHOP"));

        ArgumentCaptor<SecurityAlertDto> captor = ArgumentCaptor.forClass(SecurityAlertDto.class);
        verify(alertService).sendPushNotificationToParent(captor.capture());

        SecurityAlertDto alert = captor.getValue();
        Object dtoTriggeredCategory = readProperty(alert, "getTriggeredCategory", "triggeredCategory", "category", "type");
        assertNotNull(dtoTriggeredCategory);
        assertEquals("vape_shop", String.valueOf(dtoTriggeredCategory).toLowerCase());
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

    private Object readProperty(Object target, String... candidates) {
        for (String name : candidates) {
            try {
                Method m = target.getClass().getMethod(name);
                return m.invoke(target);
            } catch (Exception ignored) { }

            try {
                Field f = target.getClass().getDeclaredField(name);
                f.setAccessible(true);
                return f.get(target);
            } catch (Exception ignored) { }
        }
        return null;
    }
}