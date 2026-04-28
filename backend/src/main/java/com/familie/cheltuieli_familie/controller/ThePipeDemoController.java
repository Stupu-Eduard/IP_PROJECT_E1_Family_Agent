package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.service.ThePipeHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Controller special pentru DEMO.
 * Conține endpoint-uri care simulează fluxul de date prin "The Pipe".
 */
@RestController
@RequestMapping("/api/v1/demo/pipe")
@Tag(name = "The Pipe Demo", description = "Simulări în timp real pentru prezentare")
@RequiredArgsConstructor
public class ThePipeDemoController {

    private final ThePipeHandler thePipeHandler;

    @Operation(summary = "Trimite un mesaj de salut prin WebSocket")
    @GetMapping("/hello")
    public String sayHello() {
        thePipeHandler.broadcast("{\"message\": \"Salut de la The Pipe!\", \"timestamp\": \"" + LocalDateTime.now() + "\"}");
        return "OK - Mesaj de salut trimis";
    }

    @Operation(summary = "Simulează locația live a copilului (Zonă Sigură)")
    @GetMapping("/child-safe")
    public String simulateChildSafe() {
        String json = "{" +
                "\"childId\": 2," +
                "\"parentId\": 1," +
                "\"lat\": 44.4325," +
                "\"lng\": 26.1039," +
                "\"isRestricted\": false," +
                "\"timestamp\": \"" + LocalDateTime.now() + "\"" +
                "}";
        thePipeHandler.broadcast(json);
        return "OK - Locație sigură trimisă (Piața Universității)";
    }

    @Operation(summary = "Simulează intrarea copilului într-o zonă restricționată (ALERTĂ)")
    @GetMapping("/child-danger")
    public String simulateChildDanger() {
        String json = "{" +
                "\"childId\": 2," +
                "\"parentId\": 1," +
                "\"lat\": 44.4370," +
                "\"lng\": 26.0959," +
                "\"isRestricted\": true," +
                "\"timestamp\": \"" + LocalDateTime.now() + "\"" +
                "}";
        thePipeHandler.broadcast(json);
        return "OK - ALERTĂ trimisă: Copil în zonă restricționată!";
    }
}
