package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.LocationDto;
import com.familie.cheltuieli_familie.dto.UpdateLocationCoordinatesRequest;
import com.familie.cheltuieli_familie.model.Location;
import com.familie.cheltuieli_familie.repository.LocationRepository;
import com.familie.cheltuieli_familie.service.ThePipeHandler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locations")
@CrossOrigin(origins = {"https://family-agent.me", "http://localhost:5173"})
public class LocationController {

    private final LocationRepository locationRepository;
    private final ThePipeHandler thePipeHandler;

    public LocationController(LocationRepository locationRepository, ThePipeHandler thePipeHandler) {
        this.locationRepository = locationRepository;
        this.thePipeHandler = thePipeHandler;
    }

    @PostMapping("/{id}/coordinates")
    public LocationDto updateCoordinates(@PathVariable Long id, @RequestBody UpdateLocationCoordinatesRequest body) {
        int updated = locationRepository.updateCoordinates(id, body.lat(), body.lng());
        if (updated == 0) {
            throw new IllegalArgumentException("Location not found");
        }

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found"));

        return new LocationDto(location.getId(), location.getStore(), null, location.getCity(), location.getCountry(), body.lat(), body.lng());
    }
}
