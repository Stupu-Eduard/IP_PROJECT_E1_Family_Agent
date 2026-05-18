package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.GeofenceZoneResponseDto;
import com.familie.cheltuieli_familie.dto.LatLngDto;
import com.familie.cheltuieli_familie.dto.SaveGeofenceZoneRequest;
import com.familie.cheltuieli_familie.model.GeofenceZone;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.GeofenceRepository;
import com.familie.cheltuieli_familie.security.service.GeofencingService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/geofencing")
@RequiredArgsConstructor
public class GeofenceController {

    private final GeofencingService geofencingService;
    private final GeofenceRepository geofenceRepository;
    private final FamilyMemberRepository familyMemberRepository;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private Long getFamilyId(Authentication auth) {
        Long userId = ((User) auth.getPrincipal()).getId();
        return familyMemberRepository.findByUserId(userId)
                .stream().findFirst()
                .map(fm -> fm.getFamily().getId())
                .orElse(null);
    }

    @PostMapping("/zones")
    public ResponseEntity<Map<String, String>> saveZone(
            @RequestBody SaveGeofenceZoneRequest request,
            Authentication auth) {

        Long familyId = getFamilyId(auth);
        if (familyId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Părintele nu aparține niciunei familii."));
        }

        geofenceRepository.findByParentIdAndIsActiveTrue(familyId).ifPresent(z -> {
            z.setActive(false);
            geofenceRepository.save(z);
        });

        List<LatLngDto> coords = request.coordinates();
        Coordinate[] ring = new Coordinate[coords.size() + 1];
        for (int i = 0; i < coords.size(); i++) {
            ring[i] = new Coordinate(coords.get(i).lng(), coords.get(i).lat());
        }
        ring[coords.size()] = ring[0];

        GeofenceZone zone = GeofenceZone.builder()
                .parentId(familyId)
                .name(request.name() != null ? request.name() : "Zona de Siguranță")
                .area(GF.createPolygon(ring))
                .isActive(true)
                .build();

        geofenceRepository.save(zone);
        return ResponseEntity.ok(Map.of("message", "Zonă de siguranță salvată cu succes."));
    }

    @GetMapping("/zones/my")
    public ResponseEntity<Object> getMyZone(Authentication auth) {
        Long familyId = getFamilyId(auth);
        if (familyId == null) return ResponseEntity.noContent().build();

        return geofenceRepository.findByParentIdAndIsActiveTrue(familyId)
                .map(z -> {
                    Coordinate[] coords = z.getArea().getExteriorRing().getCoordinates();
                    List<LatLngDto> latLngs = Arrays.stream(coords)
                            .map(c -> new LatLngDto(c.y, c.x))
                            .toList();
                    return ResponseEntity.<Object>ok(new GeofenceZoneResponseDto(z.getId(), z.getName(), latLngs));
                })
                .orElse(ResponseEntity.noContent().build());
    }

    @DeleteMapping("/zones/my")
    public ResponseEntity<Map<String, String>> deleteMyZone(Authentication auth) {
        Long familyId = getFamilyId(auth);
        if (familyId != null) {
            geofenceRepository.findByParentIdAndIsActiveTrue(familyId).ifPresent(z -> {
                z.setActive(false);
                geofenceRepository.save(z);
            });
        }
        return ResponseEntity.ok(Map.of("message", "Zonă de siguranță eliminată."));
    }

    @PostMapping("/check-location")
    public ResponseEntity<String> checkUserLocation(@RequestBody Point locationData) {
        if (locationData == null) {
            return ResponseEntity.badRequest().body("Eroare: Datele de locație lipsesc sau sunt invalide.");
        }
        try {
            geofencingService.isUserInsideZone(locationData);
            return ResponseEntity.ok("Locația a fost recepționată și procesată.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("A apărut o eroare internă la procesarea coordonatelor.");
        }
    }
}
