package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.GeofenceZone;
import com.familie.cheltuieli_familie.service.GeofencingService;
import com.familie.cheltuieli_familie.service.NotficationService;
import org.locationtech.jts.geom.Point;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/geofence")
public class GeocenceController {

    private final GeofencingService geofencingService;
    private final NotficationService notificationService;

    public GeocenceController(GeofencingService geofencingService, NotficationService notificationService) {
        this.geofencingService = geofencingService;
        this.notificationService = notificationService;
    }

    @PostMapping("/verify")
    public String verifyLocation(@RequestBody Point userLocation, @RequestBody GeofenceZone zone) {
        boolean isInside = geofencingService.isUserInsideZone(userLocation, zone);

        if (!isInside) {
            notificationService.sendAlert("Copilul a ieșit din zona: " + zone.getName());
            return "OUTSIDE - Alertă trimisă!";
        }
        return "INSIDE - Utilizatorul este în siguranță.";
    }
}