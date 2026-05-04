package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.security.service.GeofencingService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/geofencing")
public class GeofenceController {

    private static final Logger log = LoggerFactory.getLogger(GeofenceController.class);

    private final GeofencingService geofencingService;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public GeofenceController(GeofencingService geofencingService) {
        this.geofencingService = geofencingService;
    }

    public static class LocationDto {
        private double lat; // Latitudinea (Y)
        private double lng; // Longitudinea (X)

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }
    }

    @PostMapping("/check-location")
    public ResponseEntity<String> checkUserLocation(@RequestBody LocationDto locationDto) {
        if (locationDto == null) {
            log.warn("Eroare: Datele de locație lipsesc.");
            return ResponseEntity.badRequest().body("Eroare: Datele de locație lipsesc.");
        }

        try {
            Coordinate coord = new Coordinate(locationDto.getLng(), locationDto.getLat());
            Point locationPoint = geometryFactory.createPoint(coord);

            geofencingService.processLocationUpdate(1L, 2L, locationPoint);
            log.info("Locația a fost recepționată și procesată cu succes.");

            return ResponseEntity.ok("Locația a fost recepționată și procesată.");

        } catch (Exception e) {
            log.error("Eroare internă la procesarea coordonatelor", e);
            return ResponseEntity.internalServerError().body("Eroare internă la procesarea coordonatelor.");
        }
    }
}