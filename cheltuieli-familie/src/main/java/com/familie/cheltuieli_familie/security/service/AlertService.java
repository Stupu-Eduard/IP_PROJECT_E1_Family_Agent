package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.security.model.SecurityAlertDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    /**
     * Trimite o notificare push catre parinte.
     * Momentan simuleaza trimiterea prin logare,
     * urmand sa fie integrat cu un provider real (ex: Firebase FCM, Twilio).
     */
    public void sendPushNotificationToParent(SecurityAlertDto alertDto) {
        logger.warn("!!! ALERTA DE SECURITATE TRIMISA CATRE PARINTE !!!");
        logger.warn("Parinte ID: {}", alertDto.getParentId());
        logger.warn("Copil ID: {}", alertDto.getChildId());
        logger.warn("Motiv: {}", alertDto.getAlertMessage());
        logger.warn("Categorie Restrictionata: {}", alertDto.getRestrictedCategory());
        logger.warn("Data si Ora: {}", alertDto.getTimestamp());
    }
}