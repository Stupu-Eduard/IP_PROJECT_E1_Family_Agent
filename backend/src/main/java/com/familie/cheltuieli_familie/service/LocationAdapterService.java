package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.LocationMapDto;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.MinorSafetyFilterService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LocationAdapterService {

    private final MinorSafetyFilterService minorSafetyFilterService;
    private final UserRepository userRepository;

    public LocationAdapterService(MinorSafetyFilterService minorSafetyFilterService,
                                  UserRepository userRepository) {
        this.minorSafetyFilterService = minorSafetyFilterService;
        this.userRepository = userRepository;
    }

    public LocationMapDto adapt(Long childId, Long parentId,
                                double latitude, double longitude,
                                List<String> placeTypes) {
        boolean isRestricted = minorSafetyFilterService.isLocationRestricted(placeTypes);

        String childName = userRepository.findById(childId)
                .map(u -> u.getName())
                .orElse("Copil #" + childId);

        return new LocationMapDto(
                childId,
                childName,
                parentId,
                latitude,
                longitude,
                isRestricted,
                LocalDateTime.now()
        );
    }
}