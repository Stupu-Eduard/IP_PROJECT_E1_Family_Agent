package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.repository.AlertRepository;
import com.familie.cheltuieli_familie.security.model.SecurityAlertDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertService alertService;

    private SecurityAlertDto alertDto;
    private Alert alert;

    @BeforeEach
    void setUp() {
        // Pregătim date de test folosind exact constructorul tău cu 4 parametri.
        // Observă că nu mai folosim settere, iar timestamp-ul se va genera automat!
        alertDto = new SecurityAlertDto(
                10L,
                5L,
                "Acces restrictionat detectat!",
                "JOCURI"
        );

        alert = new Alert();
        alert.setChildId(10L);
        alert.setParentId(5L);
        alert.setRead(false);
    }

    @Test
    void testSendPushNotificationToParent() {
        // Act: Apelăm metoda
        alertService.sendPushNotificationToParent(alertDto);

        // Assert: Capturăm alerta care a fost trimisă către repository.save()
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertCaptor.capture());

        // Verificăm dacă alerta salvată conține datele corecte extrase din DTO
        Alert savedAlert = alertCaptor.getValue();
        assertEquals(10L, savedAlert.getChildId());
        assertEquals(5L, savedAlert.getParentId());
        assertEquals("Acces restrictionat detectat!", savedAlert.getMessage());
        assertEquals("JOCURI", savedAlert.getRestrictedCategory());
        assertFalse(savedAlert.isRead());
    }

    @Test
    void testGetAlertsForParent() {
        // Arrange
        Long parentId = 5L;
        List<Alert> expectedAlerts = List.of(alert, new Alert());
        when(alertRepository.findByParentIdOrderByTimestampDesc(parentId)).thenReturn(expectedAlerts);

        // Act
        List<Alert> actualAlerts = alertService.getAlertsForParent(parentId);

        // Assert
        assertEquals(2, actualAlerts.size());
        verify(alertRepository, times(1)).findByParentIdOrderByTimestampDesc(parentId);
    }

    @Test
    void testGetUnreadAlertsForParent() {
        // Arrange
        Long parentId = 5L;
        List<Alert> expectedAlerts = List.of(alert);
        when(alertRepository.findByParentIdAndReadFalseOrderByTimestampDesc(parentId)).thenReturn(expectedAlerts);

        // Act
        List<Alert> actualAlerts = alertService.getUnreadAlertsForParent(parentId);

        // Assert
        assertEquals(1, actualAlerts.size());
        verify(alertRepository, times(1)).findByParentIdAndReadFalseOrderByTimestampDesc(parentId);
    }

    @Test
    void testMarkAsRead_WhenAlertExists() {
        // Arrange
        Long alertId = 99L;
        Alert mockAlert = new Alert();
        mockAlert.setRead(false); // Inițial este necitită

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(mockAlert));

        // Act
        alertService.markAsRead(alertId);

        // Assert
        assertTrue(mockAlert.isRead(), "Alerta ar trebui să fie marcată ca citită");
        verify(alertRepository, times(1)).save(mockAlert); // Verificăm că s-a salvat modificarea
    }

    @Test
    void testMarkAsRead_WhenAlertDoesNotExist() {
        // Arrange
        Long alertId = 99L;
        when(alertRepository.findById(alertId)).thenReturn(Optional.empty());

        // Act
        alertService.markAsRead(alertId);

        // Assert
        // Dacă alerta nu a fost găsită, metoda save() NU ar trebui să fie apelată niciodată
        verify(alertRepository, never()).save(any(Alert.class));
    }
}