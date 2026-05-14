package com.familie.cheltuieli_familie.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FamilyMemberDTO {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String role;
}
