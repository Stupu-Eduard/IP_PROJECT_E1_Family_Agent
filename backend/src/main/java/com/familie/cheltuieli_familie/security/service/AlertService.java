package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.repository.AlertRepository;
import com.familie.cheltuieli_familie.security.model.SecurityAlertDto;
import com.familie.cheltuieli_familie.service.FirebaseNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Am adăugat aceste 3 importuri pentru conversia timpului
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    private final AlertRepository alertRepository;
    private final FirebaseNotificationService firebaseNotificationService;

    @Value("${firebase.parent.device.token:}")
    private String parentDeviceToken;

    // Aici sunt cele 2 argumente pe care le cauta testul
    public AlertService(AlertRepository alertRepository, FirebaseNotificationService firebaseNotificationService) {
        this.alertRepository = alertRepository;
        this.firebaseNotificationService = firebaseNotificationService;
    }

    public void sendPushNotificationToParent(SecurityAlertDto alertDto) {
        Alert alert = new Alert();
        alert.setChildId(alertDto.getChildId());
        alert.setParentId(alertDto.getParentId());
        alert.setMessage(alertDto.getAlertMessage());
        alert.setRestrictedCategory(alertDto.getRestrictedCategory());

        // CONVERSIA AICI: Transformăm Long-ul în LocalDateTime
        if (alertDto.getTimestamp() != null) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(alertDto.getTimestamp()), ZoneId.systemDefault());
            alert.setTimestamp(dateTime);
        } else {
            // Dacă timestamp-ul nu există, punem ora actuală
            alert.setTimestamp(LocalDateTime.now());
        }

        alert.setRead(false);
        alertRepository.save(alert);

        logger.warn("!!! ALERTA SALVATA - Parinte ID: {}, Copil ID: {}, Categorie: {}",
                alertDto.getParentId(), alertDto.getChildId(), alertDto.getRestrictedCategory());

        if (parentDeviceToken != null && !parentDeviceToken.isEmpty()) {
            firebaseNotificationService.sendPushNotification(
                    parentDeviceToken,
                    "Alertă " + alertDto.getRestrictedCategory(),
                    alertDto.getAlertMessage()
            );
        } else {
            logger.warn("Skip sending push notification: firebase.parent.device.token is not configured");
        }
    }

    public List<Alert> getAlertsForParent(Long parentId) {
        return alertRepository.findByParentIdOrderByTimestampDesc(parentId);
    }

    public List<Alert> getUnreadAlertsForParent(Long parentId) {
        return alertRepository.findByParentIdAndReadFalseOrderByTimestampDesc(parentId);
    }

    public void markAsRead(Long alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.setRead(true);
            alertRepository.save(alert);
        });
    }
}