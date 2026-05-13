package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.response.AgentResponseDTO;
import com.familie.cheltuieli_familie.service.AgentChatService;
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

    @PostMapping("/chat")
    public ResponseEntity<AgentResponseDTO> chat(@RequestBody String userMessage) {
        log.info("Received agent chat request: {}", userMessage);
        AgentResponseDTO response = agentChatService.processQuery(userMessage);
        return ResponseEntity.ok(response);
    }
}
