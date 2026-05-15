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
        Long childId,
        String childName,
        Long parentId,
        double lat,
        double lng,
        boolean isRestricted,
        LocalDateTime timestamp
) {}