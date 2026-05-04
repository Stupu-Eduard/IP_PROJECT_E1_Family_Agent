package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.model.GeofenceZone;
import com.familie.cheltuieli_familie.repository.AlertRepository;
import com.familie.cheltuieli_familie.repository.GeofenceRepository; // <-- Decomentat
import com.familie.cheltuieli_familie.service.FirebaseNotificationService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GeofencingService {

    @Value("${firebase.parent.device.token:}")
    private String parentDeviceToken;

    private final FirebaseNotificationService firebaseNotificationService;
    private final AlertRepository alertRepository;
    private final GeofenceRepository geofenceRepository; // <-- Decomentat

    // Injectăm toate Repository-urile necesare, inclusiv cel de Geofence
    public GeofencingService(FirebaseNotificationService firebaseNotificationService,
                             AlertRepository alertRepository,
                             GeofenceRepository geofenceRepository) { // <-- Decomentat
        this.firebaseNotificationService = firebaseNotificationService;
        this.alertRepository = alertRepository;
        this.geofenceRepository = geofenceRepository; // <-- Decomentat
    }

    // 1. Metoda apelată din Controller
    public void processLocationUpdate(Long childId, Long parentId, Point locationData) {
        if (locationData == null) return;

        long startTime = System.currentTimeMillis();

        // Extragem toate zonele active direct din Baza de Date Reală
        List<GeofenceZone> activeZones = geofenceRepository.findAllByIsActiveTrue(); // <-- Activata! (Simularea a fost ștearsă)

        for (GeofenceZone zone : activeZones) {
            // Aplicăm algoritmul PIP de mare viteză
            boolean isInside = executeRayCasting(locationData, zone);

            // Dacă a ieșit din poligon, declanșăm fluxul de Audit și Alertare
            if (!isInside) {
                triggerViolationProtocol(childId, parentId, locationData, zone);
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Timp de execuție Geofence Engine: " + (endTime - startTime) + " ms");
    }

    // 2. Motorul de Calcul - Algoritmul PIP (Task 2)
    private boolean executeRayCasting(Point point, GeofenceZone zone) {
        if (zone.getArea() == null) return false;

        // Extragem direct coordonatele matematice pentru performanță maximă
        Coordinate[] vertices = zone.getArea().getCoordinates();
        double x = point.getX();
        double y = point.getY();
        boolean isInside = false;

        // Aplicăm formula de determinare a intersecției semidreptei
        for (int i = 0, j = vertices.length - 1; i < vertices.length; j = i++) {
            double xi = vertices[i].x;
            double yi = vertices[i].y;
            double xj = vertices[j].x;
            double yj = vertices[j].y;

            // Transpunerea în cod a formulelor matematice:
            boolean intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);

            if (intersect) {
                isInside = !isInside;
            }
        }
        return isInside;
    }

    // 3. Sistemul de Audit și Alertare (Task 3)
    private void triggerViolationProtocol(Long childId, Long parentId, Point location, GeofenceZone zone) {
        String alertMessage = String.format("Atenție! S-a părăsit zona: %s (Locație: %f, %f)",
                zone.getName(), location.getX(), location.getY());

        // PASUL A: Audit Logging în Baza de Date Reală
        Alert auditLog = new Alert();
        auditLog.setChildId(childId);
        auditLog.setParentId(parentId);
        auditLog.setMessage(alertMessage);
        auditLog.setRestrictedCategory("GEOFENCE_VIOLATION");
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setRead(false);

        // Salvăm dovada în DB
        alertRepository.save(auditLog);

        // PASUL B: Trimiterea notificării externe
        if (parentDeviceToken != null && !parentDeviceToken.isEmpty()) {
            firebaseNotificationService.sendPushNotification(parentDeviceToken, "Alertă Securitate", alertMessage);
        }
    }
}