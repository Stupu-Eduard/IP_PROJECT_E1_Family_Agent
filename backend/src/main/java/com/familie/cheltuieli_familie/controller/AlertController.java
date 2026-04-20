package com.familie.cheltuieli_familie.controller;
import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.security.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    /**
     * Toate alertele unui parinte, cele mai noi primele.
     * GET /api/v1/alerts?parentId=1
     */
    @GetMapping
    public List<Alert> getAlerts(@RequestParam Long parentId) {
        return alertService.getAlertsForParent(parentId);
    }

    /**
     * Doar alertele necitite.
     * GET /api/v1/alerts/unread?parentId=1
     */
    @GetMapping("/unread")
    public List<Alert> getUnreadAlerts(@RequestParam Long parentId) {
        return alertService.getUnreadAlertsForParent(parentId);
    }

    /**
     * Marcheaza o alerta ca citita.
     * PATCH /api/v1/alerts/1/read
     */
    @PatchMapping("/{alertId}/read")
    public ResponseEntity<String> markAsRead(@PathVariable Long alertId) {
        alertService.markAsRead(alertId);
        return ResponseEntity.ok("Alerta marcata ca citita.");
    }
}