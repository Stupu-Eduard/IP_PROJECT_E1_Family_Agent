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
    private final LocationValidationService locationValidationService;
    private final com.familie.cheltuieli_familie.repository.LocationRepository locationRepository;

    public ChildLocationController(LocationStreamService locationStreamService,
                                   MinorSafetyFilterService minorSafetyFilterService,
                                   LocationValidationService locationValidationService,
                                   com.familie.cheltuieli_familie.repository.LocationRepository locationRepository) {
        this.locationStreamService = locationStreamService;
        this.minorSafetyFilterService = minorSafetyFilterService;
        this.locationValidationService = locationValidationService;
        this.locationRepository = locationRepository;
    }

    @Operation(
            summary = "Sincronizeaza locatia copilului",
            description = "Primeste locatia curenta a copilului, o salvează în DB și o trimite prin fluxurile live."
    )
    @PostMapping("/location/sync")
    public ResponseEntity<String> syncLocation(
            @org.springframework.web.bind.annotation.RequestBody LocationSyncRequest request) {

        // 1. Validăm datele GPS
        if (!locationValidationService.isLocationValid(request.latitude(), request.longitude())) {
            return ResponseEntity.badRequest().body("Eroare: Locatie GPS invalida (ex: 0,0). Sincronizare oprita.");
        }

        // 2. SALVĂM ÎN BAZA DE DATE (Pentru a declanșa "The Pipe" prin Postgres NOTIFY)
        // Folosim ID-ul copilului ca referință pentru rândul din tabela 'locations'
        // NOTĂ: Într-un sistem real, am avea un tabel de 'live_locations'. Aici refolosim tabela de locations.
        locationRepository.updateCoordinates(request.childId(), request.latitude(), request.longitude());

        // 3. Trimitem prin SSE (Legacy stream)
        locationStreamService.sendLocationToParent(
                request.childId(),
                request.parentId(),
                request.latitude(),
                request.longitude(),
                request.placeTypes()
        );

        // 4. Verificăm zonele restricționate
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