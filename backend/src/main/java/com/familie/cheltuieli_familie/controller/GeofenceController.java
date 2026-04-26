package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.security.service.GeofencingService;
import org.locationtech.jts.geom.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller pentru gestionarea operațiunilor de Geofencing.
 * Folosește direct obiectul Point pentru a evita expunerea entităților persistente.
 */
@RestController
@RequestMapping("/api/geofencing")
public class GeofenceController {

    private final GeofencingService geofencingService;

    // Injectare prin constructor pentru o testabilitate mai bună
    public GeofenceController(GeofencingService geofencingService) {
        this.geofencingService = geofencingService;
    }

    /**
     * Primește locația utilizatorului sub formă de Point (coordonate X, Y).
     * * @param locationData Obiectul Point primit în format JSON de la frontend.
     * @return ResponseEntity cu statusul procesării.
     */
    @PostMapping("/check-location")
    public ResponseEntity<String> checkUserLocation(@RequestBody Point locationData) {

        // 1. Validare de bază: ne asigurăm că am primit date valide
        if (locationData == null) {
            return ResponseEntity.badRequest().body("Eroare: Datele de locație (Point) lipsesc sau sunt invalide.");
        }

        try {
            // 2. Apelăm serviciul folosind direct obiectul Point
            // Acest apel va verifica dacă punctul se află în zonele restricționate
            geofencingService.isUserInsideZone(locationData);

            // 3. Răspuns de succes generic (bună practică de securitate)
            return ResponseEntity.ok("Locația a fost recepționată și procesată.");

        } catch (Exception e) {
            // 4. Tratarea erorilor fără a expune stacktrace-ul către exterior
            return ResponseEntity.internalServerError().body("A apărut o eroare internă la procesarea coordonatelor.");
        }
    }
}