package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.model.GeofenceZone;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

@Service
public class GeofencingService {

    private final FirebaseNotificationService firebaseNotificationService;

    // Spring va injecta automat Firebase-ul aici
    public GeofencingService(FirebaseNotificationService firebaseNotificationService) {
        this.firebaseNotificationService = firebaseNotificationService;
    }

    // 1. Metoda principală, intactă, exact așa cum o aveai
    public boolean isUserInsideZone(Point userLocation, GeofenceZone zone) {
        if (userLocation == null || zone == null || zone.getArea() == null) {
            return false;
        }

        boolean isInside = zone.getArea().contains(userLocation);

        // Dacă utilizatorul NU este înăuntru, declanșăm alerta pe telefon
        if (!isInside) {
            String tokenTelefon = "token_parinte_default"; // Aici pe viitor vei pune token-ul real
            String titlu = "Alertă Geofence";
            String mesaj = "Atenție! S-a părăsit zona: " + zone.getName();

            firebaseNotificationService.sendPushNotification(tokenTelefon, titlu, mesaj);
        }

        return isInside;
    }

    // 2. Metoda secundară, forțată să ceară Point (nu Object, nu DTO)
    public void isUserInsideZone(Point locationData) {
        System.out.println("Metoda a fost apelata cu punctul exact: " + locationData);
    }
}