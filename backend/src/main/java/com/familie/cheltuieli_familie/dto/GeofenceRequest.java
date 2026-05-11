package com.familie.cheltuieli_familie.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceRequest {
    private String name;
    private List<PointDto> points;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointDto {
        private double lat;
        private double lng;
    }
}
