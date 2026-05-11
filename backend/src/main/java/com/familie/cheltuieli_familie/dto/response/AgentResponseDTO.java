package com.familie.cheltuieli_familie.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextResponseDTO.class, name = "text"),
        @JsonSubTypes.Type(value = ChartResponseDTO.class, name = "chart")
})
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class AgentResponseDTO {

    private String type;
    private String message;

    public AgentResponseDTO(String type, String message) {
        this.type = type;
        this.message = message;
    }
}
