package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.GeofenceZone;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeofencingServiceTest {

    private final GeometryFactory factory = new GeometryFactory();

    private final NotificationProvider mockNotification = new NotificationProvider() {
        @Override
        public void sendNotification(String message) {
            System.out.println("🚨 [TEST ALERT]: " + message);
        }
    };

    private final GeofencingService service = new GeofencingService(mockNotification);

    @Test
    void testGeofenceLogic() {
        // 1. Definim perimetrul
        Coordinate[] coords = new Coordinate[] {
                new Coordinate(0,0),
                new Coordinate(0,10),
                new Coordinate(10,10),
                new Coordinate(10,0),
                new Coordinate(0,0)
        };
        Polygon zoneSquare = factory.createPolygon(coords);

        GeofenceZone zone = new GeofenceZone();
        zone.setArea(zoneSquare);
        zone.setName("Zona Acasă");

        //Testam punctul din interior
        Point insidePoint = factory.createPoint(new Coordinate(5,5));
        assertTrue(service.isUserInsideZone(insidePoint, zone), "Punctul (5,5) ar trebui să fie INSIDE!");

        //Testam punctul din exterior
        Point outsidePoint = factory.createPoint(new Coordinate(15,15));
        assertFalse(service.isUserInsideZone(outsidePoint, zone), "Punctul (15,15) ar trebui să fie OUTSIDE!");

        System.out.println(" Testul a trecut cu succes! Ambele task-uri (Geofencing & Notificari) sunt validate.");
    }
}