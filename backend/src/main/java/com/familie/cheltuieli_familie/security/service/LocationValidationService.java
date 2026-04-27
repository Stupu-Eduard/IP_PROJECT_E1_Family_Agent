package com.familie.cheltuieli_familie.security.service;

import org.springframework.stereotype.Service;

@Service
public class LocationValidationService {

    /**
     * Verifica daca locatia GPS este valida.
     * Respinge locatiile implicite de eroare (ex: 0.0, 0.0 - Null Island).
     */
    public boolean isLocationValid(double latitude, double longitude) {

        // Regula 1: Daca lat si lng sunt fix 0.0, e eroare clasica de GPS lipsa.
        if (latitude == 0.0 && longitude == 0.0) {
            System.out.println("⚠️ ALERTA: Coordonate (0,0) detectate si blocate! Posibila eroare de semnal GPS.");
            return false;
        }

        // Regula 2: Verificare limite geografice reale (Pamantul)
        if (latitude < -90.0 || latitude > 90.0) {
            System.out.println("⚠️ ALERTA: Latitudine invalida: " + latitude);
            return false;
        }
        if (longitude < -180.0 || longitude > 180.0) {
            System.out.println("⚠️ ALERTA: Longitudine invalida: " + longitude);
            return false;
        }

        // Daca trece testele, e o locatie buna
        return true;
    }
}