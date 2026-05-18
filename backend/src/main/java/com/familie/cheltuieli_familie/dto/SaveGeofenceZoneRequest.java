package com.familie.cheltuieli_familie.dto;

import java.util.List;

public record SaveGeofenceZoneRequest(List<LatLngDto> coordinates, String name) {}
