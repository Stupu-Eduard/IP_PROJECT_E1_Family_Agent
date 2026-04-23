package com.familie.cheltuieli_familie.security.service;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviciul care tine conexiunile SSE deschise.
 * Cand copilul trimite o locatie noua, acest serviciu o impinge
 * catre browserul parintelui fara refresh.
 */
@Service
public class LocationStreamService {

    // Retinem conexiunea fiecarui parinte dupa ID-ul sau
    // ConcurrentHashMap = thread-safe (mai multi parinti pot fi conectati simultan)
    private final Map<Long, SseEmitter> parentEmitters = new ConcurrentHashMap<>();

    /**
     * Parintele se aboneaza la stream-ul de locatie al copilului sau.
     * Aceasta metoda e apelata cand parintele deschide dashboard-ul.
     */
    public SseEmitter subscribeParent(Long parentId) {
        // Timeout de 30 minute - dupa care conexiunea se reinnoieste automat
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        parentEmitters.put(parentId, emitter);

        // Curatam conexiunea cand parintele inchide browserul
        emitter.onCompletion(() -> parentEmitters.remove(parentId));
        emitter.onTimeout(() -> parentEmitters.remove(parentId));
        emitter.onError(e -> parentEmitters.remove(parentId));

        return emitter;
    }

    /**
     * Cand copilul trimite o locatie noua, o trimitem catre parintele sau.
     */
    public void sendLocationToParent(Long parentId, double latitude, double longitude) {
        SseEmitter emitter = parentEmitters.get(parentId);

        if (emitter == null) {
            // Parintele nu e conectat momentan, ignoram
            return;
        }

        try {
            String payload = String.format(
                    "{\"latitude\": %f, \"longitude\": %f}",
                    latitude, longitude
            );
            emitter.send(SseEmitter.event()
                    .name("location-update")
                    .data(payload));
        } catch (IOException e) {
            // Conexiunea s-a inchis neasteptat
            parentEmitters.remove(parentId);
        }
    }
}