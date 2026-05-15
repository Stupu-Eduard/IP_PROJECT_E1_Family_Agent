package com.familie.cheltuieli_familie.dto;

import java.time.LocalDateTime;

/**
 * DTO folosit pentru a trimite locatia copilului catre frontend (Google Maps SDK).
 *
 * Diferenta fata de LocationDto (care e pentru cheltuieli):
 * - Contine ID-ul copilului si al parintelui
 * - Contine flag-ul isRestricted (zona periculoasa sau nu)
 * - Contine flag-ul isOutsideGeofence (copilul a iesit din zona de siguranta setata de parinte)
 * - Contine timestamp-ul sincronizarii
 */
public record LocationMapDto(
        Long childId,
        String childName,
        Long parentId,
        double lat,
        double lng,
        boolean isRestricted,
        boolean isOutsideGeofence,
        LocalDateTime timestamp
) {}
