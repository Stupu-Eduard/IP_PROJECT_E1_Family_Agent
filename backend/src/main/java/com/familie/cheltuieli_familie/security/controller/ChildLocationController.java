package com.familie.cheltuieli_familie.security.controller;

import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import com.familie.cheltuieli_familie.security.service.LocationStreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint-ul la care copilul trimite locatia sa.
 * Dupa primire:
 *   1. Trimitem locatia catre parintele sau (SSE)
 *   2. Verificam daca locatia e intr-o zona restrictionata (alerte)
 */
@RestController
@RequestMapping("/api/v1/child")
public class ChildLocationController {

    private final LocationStreamService locationStreamService;
    private final MinorSafetyFilterService minorSafetyFilterService;

    public ChildLocationController(LocationStreamService locationStreamService,
                                   MinorSafetyFilterService minorSafetyFilterService) {
        this.locationStreamService = locationStreamService;
        this.minorSafetyFilterService = minorSafetyFilterService;
    }

    /**
     * Copilul trimite locatia sa curenta.
     *
     * Exemplu body JSON:
     * {
     *   "childId": 2,
     *   "parentId": 1,
     *   "latitude": 47.1585,
     *   "longitude": 27.6014,
     *   "placeTypes": ["bar", "restaurant"]
     * }
     */
    @PostMapping("/location/sync")
    public ResponseEntity<String> syncLocation(@RequestBody LocationSyncRequest request) {

        // 1. Trimitem locatia catre parintele conectat prin SSE
        locationStreamService.sendLocationToParent(
                request.parentId(),
                request.latitude(),
                request.longitude()
        );

        // 2. Verificam daca locatia e restrictionata si trimitem alerta
        minorSafetyFilterService.evaluateChildLocation(
                request.childId(),
                request.parentId(),
                request.placeTypes()
        );

        return ResponseEntity.ok("Locatie sincronizata cu succes.");
    }

    // Record = clasa simpla de date (Java 16+), echivalentul unui DTO
    public record LocationSyncRequest(
            Long childId,
            Long parentId,
            double latitude,
            double longitude,
            List<String> placeTypes
    ) {}
}