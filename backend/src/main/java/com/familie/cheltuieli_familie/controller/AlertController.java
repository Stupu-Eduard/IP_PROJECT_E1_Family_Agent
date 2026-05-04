package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.security.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Gestionarea alertelor de securitate - accesibile doar de PARENT")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @Operation(
            summary = "Toate alertele unui parinte",
            description = "Returneaza lista completa de alerte, sortate de la cea mai noua la cea mai veche.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de alerte returnata cu succes"),
                    @ApiResponse(responseCode = "403", description = "Acces interzis - doar PARENT")
            }
    )
    @GetMapping
    public List<Alert> getAlerts(
            @Parameter(description = "ID-ul parintelui", example = "1")
            @RequestParam Long parentId) {
        return alertService.getAlertsForParent(parentId);
    }

    @Operation(
            summary = "Alertele necitite ale unui parinte",
            description = "Returneaza doar alertele pe care parintele nu le-a citit inca.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de alerte necitite returnata cu succes"),
                    @ApiResponse(responseCode = "403", description = "Acces interzis - doar PARENT")
            }
    )
    @GetMapping("/unread")
    public List<Alert> getUnreadAlerts(
            @Parameter(description = "ID-ul parintelui", example = "1")
            @RequestParam Long parentId) {
        return alertService.getUnreadAlertsForParent(parentId);
    }

    @Operation(
            summary = "Marcheaza o alerta ca citita",
            description = "Seteaza campul 'read' pe true pentru alerta specificata.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Alerta marcata ca citita"),
                    @ApiResponse(responseCode = "403", description = "Acces interzis - doar PARENT")
            }
    )
    @PatchMapping("/{alertId}/read")
    public ResponseEntity<String> markAsRead(
            @Parameter(description = "ID-ul alertei de marcat ca citita", example = "5")
            @PathVariable Long alertId) {
        alertService.markAsRead(alertId);
        return ResponseEntity.ok("Alerta marcata ca citita.");
    }
}