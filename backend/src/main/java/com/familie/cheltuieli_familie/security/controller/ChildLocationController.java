package com.familie.cheltuieli_familie.security.controller;

import com.familie.cheltuieli_familie.security.service.LocationValidationService;
import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import com.familie.cheltuieli_familie.security.service.LocationStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/child")
@Tag(name = "Child Location", description = "Sincronizarea locatiei copilului - accesibil de CHILD si PARENT")
public class ChildLocationController {

    private final LocationStreamService locationStreamService;
    private final MinorSafetyFilterService minorSafetyFilterService;
    private final LocationValidationService locationValidationService; // <-- Paznicul nostru adaugat

    public ChildLocationController(LocationStreamService locationStreamService,
                                   MinorSafetyFilterService minorSafetyFilterService,
                                   LocationValidationService locationValidationService) {
        this.locationStreamService = locationStreamService;
        this.minorSafetyFilterService = minorSafetyFilterService;
        this.locationValidationService = locationValidationService; // <-- Injectat aici
    }

    @Operation(
            summary = "Sincronizeaza locatia copilului",
            description = "Primeste locatia curenta a copilului si o valideaza inainte de sincronizare."
    )
    @PostMapping("/location/sync")
    public ResponseEntity<String> syncLocation(
            @org.springframework.web.bind.annotation.RequestBody LocationSyncRequest request) {

        // --- PAZNICUL INTERVINE AICI ---
        // Verificam daca datele GPS sunt valide inainte sa facem orice altceva
        if (!locationValidationService.isLocationValid(request.latitude(), request.longitude())) {
            return ResponseEntity.badRequest().body("Eroare: Locatie GPS invalida (ex: 0,0). Sincronizare oprita.");
        }
        // -------------------------------

        // PASUL 1: Trimitem locatia catre parinte prin SSE ca LocationMapDto
        locationStreamService.sendLocationToParent(
                request.childId(),
                request.parentId(),
                request.latitude(),
                request.longitude(),
                request.placeTypes()
        );

        // PASUL 2: Verificam zona restrictionata si trimitem alerta daca e cazul
        minorSafetyFilterService.evaluateChildLocation(
                request.childId(),
                request.parentId(),
                request.placeTypes()
        );

        return ResponseEntity.ok("Locatie sincronizata cu succes.");
    }

    public record LocationSyncRequest(
            Long childId,
            Long parentId,
            double latitude,
            double longitude,
            List<String> placeTypes
    ) {}
}