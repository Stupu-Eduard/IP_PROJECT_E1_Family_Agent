package com.familie.cheltuieli_familie.dto;

import java.util.List;

public record GeofenceZoneResponseDto(Long id, String name, List<LatLngDto> coordinates) {}
