package com.familie.cheltuieli_familie.dto;

import java.time.LocalDateTime;

/**
 * DTO folosit pentru a trimite locatia copilului catre frontend (Google Maps SDK).
 *
 * Diferenta fata de LocationDto (care e pentru cheltuieli):
 * - Contine ID-ul copilului si al parintelui
 * - Contine flag-ul isRestricted (zona periculoasa sau nu)
 * - Contine timestamp-ul sincronizarii
 *
 * Frontend-ul lui Dimir foloseste lat si lng direct cu Google Maps SDK:
 * new google.maps.Marker({ position: { lat: dto.lat, lng: dto.lng } })
 */
public record LocationMapDto(
        Long childId,           // ID-ul copilului tracked
        Long parentId,          // ID-ul parintelui care primeste datele
        double lat,             // Latitudine - compatibil direct cu Google Maps SDK
        double lng,             // Longitudine - compatibil direct cu Google Maps SDK
        boolean isRestricted,   // true daca zona e in lista neagra (bar, casino etc.)
        LocalDateTime timestamp // Ora exacta a sincronizarii
) {}