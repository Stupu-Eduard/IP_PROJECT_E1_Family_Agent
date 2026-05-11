package com.proiect.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class ChartResponseDTO extends AgentResponseDTO {

    private ChartPayload payload;

    public ChartResponseDTO(String message, ChartPayload payload) {
        super("chart", message);
        this.payload = payload;
    }
}
