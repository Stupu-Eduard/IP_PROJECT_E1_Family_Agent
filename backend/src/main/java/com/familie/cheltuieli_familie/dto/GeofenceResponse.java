package com.familie.cheltuieli_familie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeofenceResponse {
    private Long id;
    private String name;
    private String description;
    private List<GeofenceRequest.PointDto> points;
}
