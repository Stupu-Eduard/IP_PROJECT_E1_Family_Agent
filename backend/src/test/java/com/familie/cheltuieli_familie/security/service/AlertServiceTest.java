package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.repository.AlertRepository;
import com.familie.cheltuieli_familie.security.model.SecurityAlertDto;
import com.familie.cheltuieli_familie.security.service.FirebaseNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void testSendPushNotificationToParent() {
        // Am lăsat parantezele goale aici, pentru că oricum setezi datele mai jos
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
}