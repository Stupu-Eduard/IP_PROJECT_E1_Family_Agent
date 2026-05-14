package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.model.GeofenceZone;
import com.familie.cheltuieli_familie.service.FirebaseNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GeofencingService {

    @Value("${firebase.parent.device.token:}")
    private String parentDeviceToken;

    private final FirebaseNotificationService firebaseNotificationService;

    // Spring va injecta automat Firebase-ul aici
    public GeofencingService(FirebaseNotificationService firebaseNotificationService) {
        this.firebaseNotificationService = firebaseNotificationService;
    }

    // 1. Metoda principală
    public boolean isUserInsideZone(Point userLocation, GeofenceZone zone) {
        if (userLocation == null || zone == null || zone.getArea() == null) {
            return false;
        }

        boolean isInside = zone.getArea().contains(userLocation);

        // Dacă utilizatorul NU este înăuntru, declanșăm alerta pe telefon
        if (!isInside && parentDeviceToken != null && !parentDeviceToken.isEmpty()) {
            String titlu = "Alertă Geofence";
            String mesaj = "Atenție! S-a părăsit zona: " + zone.getName();

            firebaseNotificationService.sendPushNotification(parentDeviceToken, titlu, mesaj);
        }

        return isInside;
    }

    // 2. Metoda secundară, care procesează un singur Point primit din Controller
    public void isUserInsideZone(Point locationData) {
        log.info("Metoda a fost apelata cu punctul exact: {}", locationData);
    }
}