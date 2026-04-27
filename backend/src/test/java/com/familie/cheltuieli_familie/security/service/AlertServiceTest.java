package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.repository.AlertRepository;
import com.familie.cheltuieli_familie.security.model.SecurityAlertDto;
import com.familie.cheltuieli_familie.service.FirebaseNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AlertServiceTest {

    private AlertRepository alertRepository;
    private FirebaseNotificationService firebaseNotificationService;
    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertRepository = mock(AlertRepository.class);
        firebaseNotificationService = mock(FirebaseNotificationService.class);
        alertService = new AlertService(alertRepository, firebaseNotificationService);
    }

    @Test
    void testSendPushNotificationToParent_WithTimestamp() {
        SecurityAlertDto dtoReal = new SecurityAlertDto();
        dtoReal.setChildId(2L);
        dtoReal.setParentId(1L);
        dtoReal.setAlertMessage("Test alert");
        dtoReal.setRestrictedCategory("GEOFENCING");
        dtoReal.setTimestamp(System.currentTimeMillis());

        alertService.sendPushNotificationToParent(dtoReal);

        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(firebaseNotificationService, times(1)).sendPushNotification(
                eq("token_dispozitiv_parinte"),
                eq("Alertă GEOFENCING"),
                eq("Test alert")
        );
    }

    @Test
    void testSendPushNotificationToParent_NullTimestamp() {
        // Aici testam ramura 'else' unde timestamp-ul nu este trimis
        SecurityAlertDto dtoReal = new SecurityAlertDto();
        dtoReal.setChildId(2L);
        dtoReal.setParentId(1L);
        dtoReal.setAlertMessage("Test alert no time");
        dtoReal.setRestrictedCategory("GEOFENCING");
        dtoReal.setTimestamp(null);

        alertService.sendPushNotificationToParent(dtoReal);

        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void testGetAlertsForParent() {
        Long parentId = 1L;
        List<Alert> expectedAlerts = List.of(new Alert(), new Alert());

        // Simulam ce returneaza baza de date
        when(alertRepository.findByParentIdOrderByTimestampDesc(parentId)).thenReturn(expectedAlerts);

        List<Alert> actualAlerts = alertService.getAlertsForParent(parentId);

        assertEquals(2, actualAlerts.size());
        verify(alertRepository, times(1)).findByParentIdOrderByTimestampDesc(parentId);
    }

    @Test
    void testGetUnreadAlertsForParent() {
        Long parentId = 1L;
        List<Alert> expectedAlerts = List.of(new Alert());

        when(alertRepository.findByParentIdAndReadFalseOrderByTimestampDesc(parentId)).thenReturn(expectedAlerts);

        List<Alert> actualAlerts = alertService.getUnreadAlertsForParent(parentId);

        assertEquals(1, actualAlerts.size());
        verify(alertRepository, times(1)).findByParentIdAndReadFalseOrderByTimestampDesc(parentId);
    }

    @Test
    void testMarkAsRead() {
        Long alertId = 1L;
        Alert alert = new Alert();
        alert.setRead(false); // Initial e necitita

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));

        alertService.markAsRead(alertId);

        // Verificam daca statusul a devenit true
        assertTrue(alert.isRead());
        verify(alertRepository, times(1)).findById(alertId);
        verify(alertRepository, times(1)).save(alert);
    }
}