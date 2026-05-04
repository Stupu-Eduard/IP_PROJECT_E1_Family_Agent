package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.security.service.GeofencingService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/geofencing")
public class GeofenceController {

    private final GeofencingService geofencingService;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public GeofenceController(GeofencingService geofencingService) {
        this.geofencingService = geofencingService;
    }

    // Creăm un DTO intern simplu (sau îl poți pune într-un fișier separat LocationDto.java)
    public static class LocationDto {
        public double lat; // Latitudinea (Y)
        public double lng; // Longitudinea (X)
    }

    @PostMapping("/check-location")
    public ResponseEntity<String> checkUserLocation(@RequestBody LocationDto locationDto) {
        if (locationDto == null) {
            return ResponseEntity.badRequest().body("Eroare: Datele de locație lipsesc.");
        }

        try {
            // Transformăm DTO-ul primit din JSON într-un Point matematic
            // Atenție la JTS: ordinea este (X = Longitudine, Y = Latitudine)
            Coordinate coord = new Coordinate(locationDto.lng, locationDto.lat);
            Point locationPoint = geometryFactory.createPoint(coord);

            // Apelăm serviciul (am lăsat ID-urile hardcodate cum le aveai tu)
            geofencingService.processLocationUpdate(1L, 2L, locationPoint);

            return ResponseEntity.ok("Locația a fost recepționată și procesată.");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Eroare internă la procesarea coordonatelor.");
        }
    }
}