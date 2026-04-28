package com.familie.cheltuieli_familie.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LocationValidationService {

    public boolean isLocationValid(double latitude, double longitude) {

        if (latitude == 0.0 && longitude == 0.0) {
            //Folosim log.warn pentru alerte care nu opresc aplicația, dar sunt suspecte
            log.warn("Coordonate (0,0) detectate și blocate! Posibilă eroare de semnal GPS.");
            return false;
        }

        if (latitude < -90.0 || latitude > 90.0) {
            //Putem folosi {} ca să inserăm variabile, e mai rapid și mai curat
            log.error("Latitudine invalidă detectată: {}", latitude);
            return false;
        }

        if (longitude < -180.0 || longitude > 180.0) {
            log.error("Longitudine invalidă detectată: {}", longitude);
            return false;
        }

        return true;
    }
}