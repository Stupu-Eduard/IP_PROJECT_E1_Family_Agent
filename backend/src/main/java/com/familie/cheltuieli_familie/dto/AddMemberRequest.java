package com.familie.cheltuieli_familie.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddMemberRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "Parent|Co-Parent|Child", message = "Rolul trebuie să fie Parent, Co-Parent sau Child")
    private String role;
}
