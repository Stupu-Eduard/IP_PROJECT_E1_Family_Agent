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

    // 1. Metoda principală
    public boolean isUserInsideZone(Point userLocation, GeofenceZone zone) {
        if (userLocation == null || zone == null || zone.getArea() == null) {
            return false;
        }

        boolean isInside = zone.getArea().contains(userLocation);

        // TODO: (Sprintul viitor) De extras token-ul real al parintelui din baza de date folosind un UserService.
        // Evitam erorile de Firebase cu acest token de fallback temporar.
        String tokenTelefon = "token_parinte_default";

        // Dacă utilizatorul NU este înăuntru, declanșăm alerta pe telefon
        if (!isInside) {
            String titlu = "Alertă Geofence";
            String mesaj = "Atenție! S-a părăsit zona: " + zone.getName();

            firebaseNotificationService.sendPushNotification(tokenTelefon, titlu, mesaj);
        }

        return isInside;
    }

    // 2. Metoda secundară, care procesează un singur Point primit din Controller
    public void isUserInsideZone(Point locationData) {
        System.out.println("Metoda a fost apelata cu punctul exact: " + locationData.toString());
        // TODO: De implementat logica reala de geofencing.
        // Trebuie sa comparam 'locationData' cu zonele (poligoanele) salvate in baza de date.
    }
}