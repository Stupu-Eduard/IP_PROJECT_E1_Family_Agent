package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.GeofenceRequest;
import com.familie.cheltuieli_familie.dto.GeofenceResponse;
import com.familie.cheltuieli_familie.security.service.GeofencingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/geofencing")
@CrossOrigin(origins = {"https://family-agent.me", "http://localhost:5173"})
@Tag(name = "Geofencing", description = "Gestionare zone de siguranta")
public class GeofenceController {

    private static final Logger log = LoggerFactory.getLogger(GeofenceController.class);

    private final GeofencingService geofencingService;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public GeofenceController(GeofencingService geofencingService) {
        this.geofencingService = geofencingService;
    }

    @Operation(summary = "Salveaza o zona noua")
    @PostMapping("/save-zone")
    public ResponseEntity<String> saveZone(@RequestBody GeofenceRequest request) {
        if (request == null || request.getPoints() == null || request.getPoints().isEmpty()) {
            return ResponseEntity.badRequest().body("Eroare: Poligonul nu are puncte.");
        }

        try {
            List<Coordinate> coordinates = request.getPoints().stream()
                    .map(p -> new Coordinate(p.getLng(), p.getLat()))
                    .collect(Collectors.toList());

            geofencingService.saveGeofence(request.getName() != null ? request.getName() : "Zona Desenata", coordinates);
            return ResponseEntity.ok("Zona de siguranta a fost salvata cu succes.");
        } catch (Exception e) {
            log.error("Eroare la salvarea zonei de geofence", e);
            return ResponseEntity.internalServerError().body("Eroare la salvarea zonei.");
        }
    }

    @Operation(summary = "Lista tuturor zonelor active")
    @GetMapping("/zones")
    public ResponseEntity<List<GeofenceResponse>> getAllZones() {
        try {
            List<GeofenceResponse> responses = geofencingService.getAllActiveGeofences().stream()
                    .map(zone -> {
                        List<GeofenceRequest.PointDto> points = Arrays.stream(zone.getArea().getCoordinates())
                                .map(c -> new GeofenceRequest.PointDto(c.y, c.x))
                                .collect(Collectors.toList());

                        return GeofenceResponse.builder()
                                .id(zone.getId())
                                .name(zone.getName())
                                .description(zone.getDescription())
                                .points(points)
                                .build();
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Eroare la recuperarea zonelor de geofence", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Sterge o zona specifica")
    @DeleteMapping("/zones/{id}")
    public ResponseEntity<String> deleteZone(@PathVariable Long id) {
        try {
            boolean deleted = geofencingService.deleteGeofence(id);
            if (deleted) {
                return ResponseEntity.ok("Zona a fost stearsa cu succes.");
            } else {
                return ResponseEntity.status(404).body("Eroare: Zona nu a fost gasita.");
            }
        } catch (Exception e) {
            log.error("Eroare la stergerea zonei", e);
            return ResponseEntity.internalServerError().body("Eroare la stergerea zonei.");
        }
    }

    @Operation(summary = "Sterge toate zonele (Sterge tot)")
    @DeleteMapping("/zones/all")
    public ResponseEntity<String> deleteAllZones() {
        try {
            geofencingService.deleteAllGeofences();
            return ResponseEntity.ok("Toate zonele au fost sterse cu succes.");
        } catch (Exception e) {
            log.error("Eroare la stergerea tuturor zonelor", e);
            return ResponseEntity.internalServerError().body("Eroare la stergerea tuturor zonelor.");
        }
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
            log.warn("Eroare: Datele de locatie lipsesc.");
            return ResponseEntity.badRequest().body("Eroare: Datele de locatie lipsesc.");
        }

        try {
            Coordinate coord = new Coordinate(locationDto.getLng(), locationDto.getLat());
            Point locationPoint = geometryFactory.createPoint(coord);

            geofencingService.processLocationUpdate(1L, 2L, locationPoint);
            log.info("Locatia a fost receptionata si procesata cu succes.");

            return ResponseEntity.ok("Locatia a fost receptionata si procesata.");

        } catch (Exception e) {
            log.error("Eroare interna la procesarea coordonatelor", e);
            return ResponseEntity.internalServerError().body("Eroare interna la procesarea coordonatelor.");
        }
    }
}