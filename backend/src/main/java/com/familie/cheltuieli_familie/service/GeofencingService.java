package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.GeofenceZone;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

@Service
public class GeofencingService {

    private final NotificationProvider notificationProvider;

    // Spring va injecta automat MockNotificationService aici
    public GeofencingService(NotificationProvider notificationProvider) {
        this.notificationProvider = notificationProvider;
    }

    public boolean isUserInsideZone(Point userLocation, GeofenceZone zone) {
        if (userLocation == null || zone == null || zone.getArea() == null) {
            return false;
        }

        boolean isInside = zone.getArea().contains(userLocation);

        // Dacă utilizatorul NU este înăuntru, declanșăm alerta
        if (!isInside) {
            notificationProvider.sendNotification("Atenție! S-a părăsit zona: " + zone.getName());
        }

        return isInside;
    }
}