package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.response.AgentResponseDTO;
import com.familie.cheltuieli_familie.service.AgentChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/agent")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AgentController {

    private final AgentChatService agentChatService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/chat")
    public ResponseEntity<AgentResponseDTO> chat(@RequestBody String userMessage) {
        String cleanMessage = extractMessage(userMessage);
        log.info("Received agent chat request: {}", cleanMessage);
        AgentResponseDTO response = agentChatService.processQuery(cleanMessage);
        return ResponseEntity.ok(response);
    }

    private String extractMessage(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return rawBody;
        }
        rawBody = rawBody.trim();
        // If it's a JSON object with a "message" field, extract it
        if (rawBody.startsWith("{") && rawBody.contains("\"message\"")) {
            try {
                JsonNode node = objectMapper.readTree(rawBody);
                JsonNode messageNode = node.path("message");
                if (!messageNode.isMissingNode() && messageNode.isTextual()) {
                    return messageNode.asText();
                }
            } catch (Exception e) {
                log.debug("Failed to parse JSON body, using raw text: {}", e.getMessage());
            }
        }
        return rawBody;
    }
}
