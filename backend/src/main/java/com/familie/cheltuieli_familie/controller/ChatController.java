package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.response.AgentResponseDTO;
import com.familie.cheltuieli_familie.service.AgentChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/chat")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatController {

    private final AgentChatService agentChatService;

    @Data
    public static class ChatRequest {
        @NotBlank(message = "Message cannot be empty")
        private String message;
    }

    /**
     * POST /v1/chat
     * Primește mesajul utilizatorului și returnează AgentResponseDTO
     * cu type: "text" | "chart" — compatibil cu frontendrul.
     */
    @PostMapping
    public ResponseEntity<AgentResponseDTO> chat(@Valid @RequestBody ChatRequest req) {
        log.info("Chat message received: {}", req.getMessage());
        AgentResponseDTO response = agentChatService.processQuery(req.getMessage());
        return ResponseEntity.ok(response);
    }
}
