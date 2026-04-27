package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.security.model.SecurityAlertDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class MinorSafetyFilterService {

    private static final Logger logger = LoggerFactory.getLogger(MinorSafetyFilterService.class);
    private final AlertService alertService;

    private static final Set<String> RESTRICTED_CATEGORIES = Set.of(
            "bar", "liquor_store", "night_club", "casino", "vape_shop"
    );

    public MinorSafetyFilterService(AlertService alertService) {
        this.alertService = alertService;
    }

    public void evaluateChildLocation(Long childId, Long parentId, List<String> placeTypes) {
        if (placeTypes == null || placeTypes.isEmpty()) {
            return;
        }

        logger.info("Evaluam locatia pentru copilul {}...", childId);

        String triggeredCategory = placeTypes.stream()
                .map(String::toLowerCase)
                .filter(RESTRICTED_CATEGORIES::contains)
                .findFirst()
                .orElse(null);

        if (triggeredCategory != null) {
            String message = String.format(
                    "Atentie! Minorul a inregistrat o locatie intr-o zona restrictionata de tip '%s'.",
                    triggeredCategory
            );

            // REPARATIE AICI: Folosim constructorul gol si setteri pentru a evita eroarea de "Cannot resolve constructor"
            SecurityAlertDto alert = new SecurityAlertDto();
            alert.setChildId(childId);
            alert.setParentId(parentId);
            alert.setAlertMessage(message);
            alert.setRestrictedCategory(triggeredCategory);
            alert.setTimestamp(System.currentTimeMillis()); // Adaugam si timpul curent

            alertService.sendPushNotificationToParent(alert);
        } else {
            logger.info("Locatie sigura. Nicio alerta declansata.");
        }
    }

    public boolean isLocationRestricted(List<String> placeTypes) {
        if (placeTypes == null || placeTypes.isEmpty()) return false;
        return placeTypes.stream()
                .map(String::toLowerCase)
                .anyMatch(RESTRICTED_CATEGORIES::contains);
    }
}