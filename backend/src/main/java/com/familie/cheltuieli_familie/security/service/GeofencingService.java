package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.model.GeofenceZone;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

@Service
public class GeofencingService {

    // 1. Cerem direct serviciul de Firebase în loc de vechiul Provider
    private final FirebaseNotificationService firebaseNotificationService;

    // 2. Spring va injecta automat Firebase-ul aici
    public GeofencingService(FirebaseNotificationService firebaseNotificationService) {
        this.firebaseNotificationService = firebaseNotificationService;
    }

    public boolean isUserInsideZone(Point userLocation, GeofenceZone zone) {
        if (userLocation == null || zone == null || zone.getArea() == null) {
            return false;
        }

        boolean isInside = zone.getArea().contains(userLocation);

        // Dacă utilizatorul NU este înăuntru, declanșăm alerta
        if (!isInside) {
            // Adaptăm pentru Firebase: are nevoie de token, titlu și corp mesaj
            String tokenTelefon = "token_parinte_default"; // Aici pe viitor vei pune token-ul real din baza de date
            String titlu = "Alertă Geofence";
            String mesaj = "Atenție! S-a părăsit zona: " + zone.getName();

            firebaseNotificationService.sendPushNotification(tokenTelefon, titlu, mesaj);
        }

        return isInside;
    }

    public void isUserInsideZone(Object locationData) {
        System.out.println("Metoda a fost apelata: " + locationData);
    }
}