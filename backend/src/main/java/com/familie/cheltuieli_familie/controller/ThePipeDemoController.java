package com.familie.cheltuieli_familie.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familie.cheltuieli_familie.service.ThePipeHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller special pentru DEMO.
 * Conține endpoint-uri care simulează fluxul de date prin "The Pipe".
 */
@RestController
@RequestMapping("/api/v1/demo/pipe")
@Tag(name = "The Pipe Demo", description = "Simulări în timp real pentru prezentare")
@RequiredArgsConstructor
public class ThePipeDemoController {

    private static final String TIMESTAMP_KEY = "timestamp";

    private final ThePipeHandler thePipeHandler;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Trimite un mesaj de salut prin WebSocket")
    @PostMapping("/hello")
    public String sayHello() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(Map.of(
                "message", "Salut de la The Pipe!",
                TIMESTAMP_KEY, LocalDateTime.now().toString()
        ));
        thePipeHandler.broadcast(json);
        return "OK - Mesaj de salut trimis";
    }

    @Operation(summary = "Simulează locația live a copilului (Zonă Sigură)")
    @PostMapping("/child-safe")
    public String simulateChildSafe() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(Map.of(
                "childId", 2,
                "parentId", 1,
                "lat", 44.4325,
                "lng", 26.1039,
                "isRestricted", false,
                TIMESTAMP_KEY, LocalDateTime.now().toString()
        ));
        thePipeHandler.broadcast(json);
        return "OK - Locatie sigura trimisa (Piata Universitatii)";
    }

    @Operation(summary = "Simulează intrarea copilului într-o zonă restricționată (ALERTĂ)")
    @PostMapping("/child-danger")
    public String simulateChildDanger() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(Map.of(
                "childId", 2,
                "parentId", 1,
                "lat", 44.4370,
                "lng", 26.0959,
                "isRestricted", true,
                TIMESTAMP_KEY, LocalDateTime.now().toString()
        ));
        thePipeHandler.broadcast(json);
        return "OK - ALERTA trimisa: Copil in zona restrictionata!";
    }
}
