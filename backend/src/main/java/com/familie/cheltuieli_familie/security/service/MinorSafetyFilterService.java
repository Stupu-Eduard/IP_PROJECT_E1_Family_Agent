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

    /**
     * Evalueaza o locatie vizitata de copil.
     * Daca contine categorii interzise, declanseaza alerta catre parinte.
     */
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
            SecurityAlertDto alert = new SecurityAlertDto(childId, parentId, message, triggeredCategory);
            alertService.sendPushNotificationToParent(alert);
        } else {
            logger.info("Locatie sigura. Nicio alerta declansata.");
        }
    }

    /**
     * Verifica simplu daca o lista de tipuri de locuri contine categorii restrictionate.
     */
    public boolean isLocationRestricted(List<String> placeTypes) {
        if (placeTypes == null || placeTypes.isEmpty()) return false;
        return placeTypes.stream()
                .map(String::toLowerCase)
                .anyMatch(RESTRICTED_CATEGORIES::contains);
    }
}