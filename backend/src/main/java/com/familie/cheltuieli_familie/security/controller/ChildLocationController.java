package com.familie.cheltuieli_familie.security.controller;

import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import com.familie.cheltuieli_familie.security.service.LocationStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    public ChildLocationController(LocationStreamService locationStreamService,
                                   MinorSafetyFilterService minorSafetyFilterService) {
        this.locationStreamService = locationStreamService;
        this.minorSafetyFilterService = minorSafetyFilterService;
    }

    @Operation(
            summary = "Sincronizeaza locatia copilului",
            description = """
                    Primeste locatia curenta a copilului si:
                    1. O transforma prin LocationAdapterService intr-un LocationMapDto
                    2. O trimite in timp real catre parintele conectat prin SSE
                    3. Verifica daca locatia e intr-o zona restrictionata si trimite alerta
                    
                    **Categorii restrictionate:** bar, liquor_store, night_club, casino, vape_shop
                    """,
            requestBody = @RequestBody(
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "childId": 2,
                                              "parentId": 1,
                                              "latitude": 47.1585,
                                              "longitude": 27.6014,
                                              "placeTypes": ["bar", "restaurant"]
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Locatie sincronizata cu succes"),
                    @ApiResponse(responseCode = "403", description = "Acces interzis")
            }
    )
    @PostMapping("/location/sync")
    public ResponseEntity<String> syncLocation(
            @org.springframework.web.bind.annotation.RequestBody LocationSyncRequest request) {

        // PASUL 1: Trimitem locatia catre parinte prin SSE ca LocationMapDto
        // (acum trimite obiect structurat in loc de JSON string manual)
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