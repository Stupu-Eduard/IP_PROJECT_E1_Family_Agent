package com.familie.cheltuieli_familie.dto;

public record InvitationDTO(
        Long id,
        Long familyId,
        String familyName,
        String invitedByName,
        String role
) {}
