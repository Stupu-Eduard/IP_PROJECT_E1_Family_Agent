package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.LocationMapDto;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.GeofenceRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LocationAdapterService {

    private final MinorSafetyFilterService minorSafetyFilterService;
    private final UserRepository userRepository;
    private final GeofenceRepository geofenceRepository;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    public LocationAdapterService(MinorSafetyFilterService minorSafetyFilterService,
                                  UserRepository userRepository,
                                  GeofenceRepository geofenceRepository) {
        this.minorSafetyFilterService = minorSafetyFilterService;
        this.userRepository = userRepository;
        this.geofenceRepository = geofenceRepository;
    }

    public LocationMapDto adapt(Long childId, Long parentId,
                                double latitude, double longitude,
                                List<String> placeTypes) {
        boolean isRestricted = minorSafetyFilterService.isLocationRestricted(placeTypes);

        String childName = userRepository.findById(childId)
                .map(User::getName)
                .orElse("Copil #" + childId);

        boolean isOutsideGeofence = geofenceRepository
                .findByParentIdAndIsActiveTrue(parentId)
                .map(zone -> {
                    Point point = GF.createPoint(new Coordinate(longitude, latitude));
                    point.setSRID(4326);
                    return !zone.getArea().contains(point);
                })
                .orElse(false);

        return new LocationMapDto(
                childId,
                childName,
                parentId,
                latitude,
                longitude,
                isRestricted,
                isOutsideGeofence,
                LocalDateTime.now()
        );
    }
}
