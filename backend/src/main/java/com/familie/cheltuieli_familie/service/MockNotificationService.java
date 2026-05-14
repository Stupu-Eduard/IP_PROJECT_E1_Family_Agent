package com.familie.cheltuieli_familie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockNotificationService implements NotificationProvider {

    @Override
    public void sendNotification(String message) {
        log.info("[ALERTA GEOFENCE]: {}", message);
        log.info("[SISTEM]: Trimitere simulata prin Firebase/SMS...");
        log.info("[STATUS]: Notificare livrata cu succes.");
    }
}