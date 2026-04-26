package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.dto.LocationMapDto;
import com.familie.cheltuieli_familie.service.LocationAdapterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviciul care tine conexiunile SSE deschise.
 * Cand copilul trimite o locatie noua, acest serviciu o impinge
 * catre browserul parintelui fara refresh.
 *
 * Acum foloseste LocationAdapterService pentru a transforma
 * coordonatele brute intr-un LocationMapDto structurat,
 * gata de folosit de Google Maps SDK pe frontend.
 */
@Service
public class LocationStreamService {

    private final Map<Long, SseEmitter> parentEmitters = new ConcurrentHashMap<>();
    private final LocationAdapterService locationAdapterService;

    // ObjectMapper = unealta Jackson care transforma obiecte Java in JSON
    // JavaTimeModule = modul care stie sa serializeze LocalDateTime corect
    private final ObjectMapper objectMapper;

    public LocationStreamService(LocationAdapterService locationAdapterService) {
        this.locationAdapterService = locationAdapterService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Parintele se aboneaza la stream-ul de locatie al copilului sau.
     * Aceasta metoda e apelata cand parintele deschide dashboard-ul.
     */
    public SseEmitter subscribeParent(Long parentId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        parentEmitters.put(parentId, emitter);

        emitter.onCompletion(() -> parentEmitters.remove(parentId));
        emitter.onTimeout(() -> parentEmitters.remove(parentId));
        emitter.onError(e -> parentEmitters.remove(parentId));

        return emitter;
    }

    /**
     * Cand copilul trimite o locatie noua, o transformam prin adaptor
     * si o trimitem ca LocationMapDto catre parintele conectat.
     *
     * Inainte: trimitea un String JSON manual formatat
     * Acum: foloseste LocationAdapterService care produce un obiect
     *       structurat cu lat, lng, isRestricted, timestamp
     *       gata de folosit direct de Google Maps SDK pe frontend
     *
     * @param childId    - ID-ul copilului
     * @param parentId   - ID-ul parintelui
     * @param latitude   - latitudine
     * @param longitude  - longitudine
     * @param placeTypes - tipurile de locuri (pentru verificarea zonei restrictionate)
     */
    public void sendLocationToParent(Long childId, Long parentId,
                                     double latitude, double longitude,
                                     List<String> placeTypes) {
        SseEmitter emitter = parentEmitters.get(parentId);

        if (emitter == null) {
            // Parintele nu e conectat momentan, ignoram
            return;
        }

        try {
            // Transformam datele brute intr-un DTO structurat prin adaptor
            LocationMapDto dto = locationAdapterService.adapt(
                    childId, parentId, latitude, longitude, placeTypes
            );

            // Serializam DTO-ul in JSON si il trimitem prin SSE
            // Rezultat JSON pentru frontend:
            // {
            //   "childId": 2,
            //   "parentId": 1,
            //   "lat": 47.1585,
            //   "lng": 27.6014,
            //   "isRestricted": true,
            //   "timestamp": "2026-04-26T10:30:00"
            // }
            String payload = objectMapper.writeValueAsString(dto);

            emitter.send(SseEmitter.event()
                    .name("location-update")
                    .data(payload));

        } catch (IOException e) {
            // Conexiunea s-a inchis neasteptat
            parentEmitters.remove(parentId);
        }
    }
}