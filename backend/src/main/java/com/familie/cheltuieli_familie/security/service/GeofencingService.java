package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.model.GeofenceZone;
import com.familie.cheltuieli_familie.repository.AlertRepository;
import com.familie.cheltuieli_familie.repository.GeofenceRepository;
import com.familie.cheltuieli_familie.service.FirebaseNotificationService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class GeofencingService {

    @Value("${firebase.parent.device.token:}")
    private String parentDeviceToken;

    private final FirebaseNotificationService firebaseNotificationService;
    private final AlertRepository alertRepository;
    private final GeofenceRepository geofenceRepository;
    private final org.locationtech.jts.geom.GeometryFactory geometryFactory = new org.locationtech.jts.geom.GeometryFactory(new org.locationtech.jts.geom.PrecisionModel(), 4326);

    public GeofencingService(FirebaseNotificationService firebaseNotificationService,
                             AlertRepository alertRepository,
                             GeofenceRepository geofenceRepository) {
        this.firebaseNotificationService = firebaseNotificationService;
        this.alertRepository = alertRepository;
        this.geofenceRepository = geofenceRepository;
    }

    public void saveGeofence(String name, List<Coordinate> coordinates) {
        if (coordinates.size() < 3) {
            throw new IllegalArgumentException("Un poligon are nevoie de cel putin 3 puncte.");
        }

        // Close the polygon if not closed
        if (!coordinates.get(0).equals(coordinates.get(coordinates.size() - 1))) {
            coordinates.add(new Coordinate(coordinates.get(0).x, coordinates.get(0).y));
        }

        if (coordinates.size() < 4) {
             throw new IllegalArgumentException("Datele poligonului sunt invalide pentru crearea unei arii.");
        }

        org.locationtech.jts.geom.Polygon polygon = geometryFactory.createPolygon(coordinates.toArray(new Coordinate[0]));

        GeofenceZone zone = GeofenceZone.builder()
                .name(name)
                .area(polygon)
                .isActive(true)
                .build();
        geofenceRepository.save(zone);
    }

    public List<GeofenceZone> getAllActiveGeofences() {
        return geofenceRepository.findAllByIsActiveTrue();
    }

    public boolean deleteGeofence(Long id) {
        return geofenceRepository.findById(id).map(zone -> {
            zone.setActive(false);
            geofenceRepository.save(zone);
            return true;
        }).orElse(false);
    }

    public void deleteAllGeofences() {
        List<GeofenceZone> allActive = geofenceRepository.findAllByIsActiveTrue();
        allActive.forEach(z -> z.setActive(false));
        geofenceRepository.saveAll(allActive);
    }

    public void checkExpense(Long expenseId, String store, Double lat, Double lng) {
        if (lat == null || lng == null) return;

        Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
        List<GeofenceZone> activeZones = geofenceRepository.findAllByIsActiveTrue();

        for (GeofenceZone zone : activeZones) {
            if (executeRayCasting(point, zone)) {
                triggerSecurityAlert(expenseId, store, zone);
            }
        }
    }

    private void triggerSecurityAlert(Long expenseId, String store, GeofenceZone zone) {
        String message = String.format("ALERTA SECURITATE: Cheltuiala inregistrata in zona restrictionata: %s (Magazin: %s)", 
                zone.getName(), store);

        Alert alert = new Alert();
        alert.setMessage(message);
        alert.setRestrictedCategory("SECURITY_ZONE_VIOLATION");
        alert.setTimestamp(LocalDateTime.now());
        alert.setRead(false);
        // IDs are hardcoded in processLocationUpdate as well, keeping consistency for now
        alert.setChildId(1L); 
        alert.setParentId(2L);
        
        // Penalizare 50 RON pentru spending in zona interzisa
        alert.setExtraCost(java.math.BigDecimal.valueOf(50.00));

        alertRepository.save(alert);
    }

    public void processLocationUpdate(Long childId, Long parentId, Point locationData) {
        if (locationData == null) return;

        long startTime = System.currentTimeMillis();

        List<GeofenceZone> activeZones = geofenceRepository.findAllByIsActiveTrue();

        for (GeofenceZone zone : activeZones) {
            boolean isInside = executeRayCasting(locationData, zone);

            if (!isInside) {
                triggerViolationProtocol(childId, parentId, locationData, zone);
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Timp de execuție Geofence Engine: " + (endTime - startTime) + " ms");
    }

    private boolean executeRayCasting(Point point, GeofenceZone zone) {
        if (zone.getArea() == null) return false;

        Coordinate[] vertices = zone.getArea().getCoordinates();
        double x = point.getX();
        double y = point.getY();
        boolean isInside = false;

        for (int i = 0, j = vertices.length - 1; i < vertices.length; j = i++) {
            double xi = vertices[i].x;
            double yi = vertices[i].y;
            double xj = vertices[j].x;
            double yj = vertices[j].y;

            boolean intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);

            if (intersect) {
                isInside = !isInside;
            }
        }
        return isInside;
    }

    private void triggerViolationProtocol(Long childId, Long parentId, Point location, GeofenceZone zone) {
        String alertMessage = String.format("Atentie! S-a parasit zona: %s (Locatie: %f, %f)",
                zone.getName(), location.getX(), location.getY());

        Alert auditLog = new Alert();
        auditLog.setChildId(childId);
        auditLog.setParentId(parentId);
        auditLog.setMessage(alertMessage);
        auditLog.setRestrictedCategory("GEOFENCE_VIOLATION");
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setRead(false);
        auditLog.setExtraCost(java.math.BigDecimal.valueOf(10.00)); // Penalizare 10 RON per iesire

        alertRepository.save(auditLog);

        if (parentDeviceToken != null && !parentDeviceToken.isEmpty()) {
            firebaseNotificationService.sendPushNotification(parentDeviceToken, "Alerta Securitate", alertMessage);
        }
    }
}
