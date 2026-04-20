package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.repository.AlertRepository;
import com.familie.cheltuieli_familie.security.model.SecurityAlertDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public void sendPushNotificationToParent(SecurityAlertDto alertDto) {
        Alert alert = new Alert();
        alert.setChildId(alertDto.getChildId());
        alert.setParentId(alertDto.getParentId());
        alert.setMessage(alertDto.getAlertMessage());
        alert.setRestrictedCategory(alertDto.getRestrictedCategory());
        alert.setTimestamp(alertDto.getTimestamp());
        alert.setRead(false);
        alertRepository.save(alert);

        logger.warn("!!! ALERTA SALVATA - Parinte ID: {}, Copil ID: {}, Categorie: {}",
                alertDto.getParentId(), alertDto.getChildId(), alertDto.getRestrictedCategory());
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