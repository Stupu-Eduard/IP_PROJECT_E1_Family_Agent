package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.security.service.GeofencingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/geofencing")
public class GeofenceController {

    private final GeofencingService geofencingService;

    public GeofenceController(GeofencingService geofencingService) {
        this.geofencingService = geofencingService;
    }

    @PostMapping("/check-location")
    public ResponseEntity<String> checkUserLocation(@RequestBody Object locationData) {
        // 1. Validare de securitate (Nota A)
        if (locationData == null) {
            return ResponseEntity.badRequest().body("Date invalide: Locatia nu poate fi nula.");
        }

        try {
            //  Apelăm motorul de geofencing.
            // Dacă utilizatorul e OUTSIDE, acest service va apela automat AlertService
            // și Firebase-ul
            geofencingService.isUserInsideZone(locationData);

            // 3. Returnăm mereu un mesaj OK generic pentru a nu da informații atacatorilor
            return ResponseEntity.ok("Locatie receptionata si procesata.");

        } catch (Exception e) {
            // Fără e.printStackTrace() pentru securitate!
            return ResponseEntity.internalServerError().body("A apărut o eroare internă la procesare.");
        }
    }
}