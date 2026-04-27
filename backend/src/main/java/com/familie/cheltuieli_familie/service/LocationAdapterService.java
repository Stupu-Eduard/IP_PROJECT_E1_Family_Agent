package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.LocationMapDto;
import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Adaptorul de date pentru Google Maps SDK.
 *
 * Adaptor = design pattern care transforma datele dintr-un format in altul.
 * Aici transformam coordonatele brute primite de la copil
 * intr-un obiect LocationMapDto gata de folosit de frontend.
 *
 * Fara adaptor, frontend-ul ar primi date brute si ar trebui
 * sa faca el transformarile - ceea ce e mai greu si mai error-prone.
 */
@Service
public class LocationAdapterService {

    private final MinorSafetyFilterService minorSafetyFilterService;

    public LocationAdapterService(MinorSafetyFilterService minorSafetyFilterService) {
        this.minorSafetyFilterService = minorSafetyFilterService;
    }

    /**
     * Transforma coordonatele brute din DB intr-un LocationMapDto
     * gata de trimis catre frontend prin SSE.
     *
     * @param childId    - ID-ul copilului
     * @param parentId   - ID-ul parintelui
     * @param latitude   - latitudine bruta din DB
     * @param longitude  - longitudine bruta din DB
     * @param placeTypes - tipurile de locuri returnate de Google Maps
     * @return LocationMapDto - obiect curat pentru frontend
     */
    public LocationMapDto adapt(Long childId, Long parentId,
                                double latitude, double longitude,
                                List<String> placeTypes) {
        // Verificam daca zona e restrictionata folosind serviciul existent
        boolean isRestricted = minorSafetyFilterService.isLocationRestricted(placeTypes);

        // Construim si returnam DTO-ul curat pentru Google Maps SDK
        return new LocationMapDto(
                childId,
                parentId,
                latitude,   // lat - compatibil direct cu { lat, lng } din Google Maps
                longitude,  // lng - compatibil direct cu { lat, lng } din Google Maps
                isRestricted,
                LocalDateTime.now()
        );
    }
}