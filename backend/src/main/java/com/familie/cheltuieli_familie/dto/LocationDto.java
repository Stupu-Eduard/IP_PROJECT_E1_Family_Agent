package com.familie.cheltuieli_familie.dto;

public record LocationDto(
        Long id,
        String store,
        String address,
        String city,
        String country,
        Double lat,
        Double lng
) {
}
