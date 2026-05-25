package com.familie.cheltuieli_familie.security.controller;

import com.familie.cheltuieli_familie.security.service.LocationValidationService;
import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import com.familie.cheltuieli_familie.security.service.LocationStreamService;
import com.familie.cheltuieli_familie.security.util.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/child")
@Tag(name = "Child Location", description = "Sincronizarea locatiei copilului - accesibil de CHILD si PARENT")
@Slf4j
public class ChildLocationController {

    private final LocationStreamService locationStreamService;
    private final MinorSafetyFilterService minorSafetyFilterService;
    private final LocationValidationService locationValidationService;
    private final com.familie.cheltuieli_familie.repository.LocationRepository locationRepository;
    private final SecurityService securityService;

    public ChildLocationController(LocationStreamService locationStreamService,
                                   MinorSafetyFilterService minorSafetyFilterService,
                                   LocationValidationService locationValidationService,
                                   com.familie.cheltuieli_familie.repository.LocationRepository locationRepository,
                                   SecurityService securityService) {
        this.locationStreamService = locationStreamService;
        this.minorSafetyFilterService = minorSafetyFilterService;
        this.locationValidationService = locationValidationService;
        this.locationRepository = locationRepository;
        this.securityService = securityService;
    }

    @Operation(
            summary = "Sincronizeaza locatia copilului",
            description = "Primeste locatia curenta a copilului, o salvează în DB și o trimite prin fluxurile live."
    )
    @PostMapping("/location/sync")
    public ResponseEntity<String> syncLocation(
            @org.springframework.web.bind.annotation.RequestBody LocationSyncRequest request) {

        if (!locationValidationService.isLocationValid(request.latitude(), request.longitude())) {
            return ResponseEntity.badRequest().body("Eroare: Locatie GPS invalida (ex: 0,0). Sincronizare oprita.");
        }

        // Extrage childId și familyId din SecurityContext (nu din body — mai sigur și corect)
        Long[] scope = securityService.resolveScope();
        Long familyId = scope[0];
        Long childId = scope[1];

        if (childId == null) {
            log.warn("Sync locatie fără user autentificat");
            return ResponseEntity.status(401).body("Neautorizat.");
        }

        // Salvare în DB opțională — nu blochează notificarea live dacă eșuează
        try {
            locationRepository.updateCoordinates(childId, request.latitude(), request.longitude());
        } catch (Exception e) {
            log.warn("Nu s-a putut actualiza coordonata în DB pentru childId={}: {}", childId, e.getMessage());
        }

        if (familyId == null) {
            log.warn("Copilul {} nu are familie asociată, nu se poate trimite locația live.", childId);
            return ResponseEntity.ok("Locatie validată, dar copilul nu aparține unei familii.");
        }

        // Trimitem prin WebSocket (The Pipe) și SSE
        locationStreamService.sendLocationToParent(
                childId,
                familyId,
                request.latitude(),
                request.longitude(),
                request.placeTypes()
        );

        // Verificăm zonele restricționate
        minorSafetyFilterService.evaluateChildLocation(
                childId,
                familyId,
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
