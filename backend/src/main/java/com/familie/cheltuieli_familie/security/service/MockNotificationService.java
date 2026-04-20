package com.familie.cheltuieli_familie.security.service;

import org.springframework.stereotype.Service;

@Service
public class MockNotificationService implements NotificationProvider {

    @Override
    public void sendNotification(String message) {
        System.out.println("\n==========================================");
        System.out.println("🚨 [ALERTA GEOFENCE]: " + message);
        System.out.println("📱 [SISTEM]: Trimitere simulată prin Firebase/SMS...");
        System.out.println("✅ [STATUS]: Notificare livrată cu succes.");
        System.out.println("==========================================\n");
    }
}