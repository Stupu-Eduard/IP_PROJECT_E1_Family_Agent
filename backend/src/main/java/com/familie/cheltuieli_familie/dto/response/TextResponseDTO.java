package com.familie.cheltuieli_familie.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class TextResponseDTO extends AgentResponseDTO {

    public TextResponseDTO(String message) {
        super("text", message);
    }
}
