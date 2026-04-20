package com.familie.cheltuieli_familie.security.controller;


import com.familie.cheltuieli_familie.security.service.LocationStreamService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Endpoint-ul la care parintele se conecteaza pentru a primi
 * locatia copilului in timp real.
 *
 * Exemplu: GET /api/v1/parent/location-stream?parentId=1
 */
@RestController
@RequestMapping("/api/v1/parent")
public class LocationSseController {

    private final LocationStreamService locationStreamService;

    public LocationSseController(LocationStreamService locationStreamService) {
        this.locationStreamService = locationStreamService;
    }

    /**
     * Parintele se conecteaza la acest endpoint o singura data.
     * Dupa aceea, serverul ii trimite automat fiecare update de locatie.
     */
    @GetMapping(value = "/location-stream", produces = "text/event-stream")
    public SseEmitter streamLocation(@RequestParam Long parentId) {
        return locationStreamService.subscribeParent(parentId);
    }
}
