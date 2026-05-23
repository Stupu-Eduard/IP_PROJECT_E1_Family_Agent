package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.response.AgentResponseDTO;
import com.familie.cheltuieli_familie.security.ChatRateLimiter;
import com.familie.cheltuieli_familie.service.AgentChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/chat")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatController {

    private final AgentChatService agentChatService;
    private final ChatRateLimiter rateLimiter;

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
    public ResponseEntity<Object> chat(@Valid @RequestBody ChatRequest req) {
        String userId = getCurrentUserId();
        if (!rateLimiter.allowRequest(userId)) {
            long waitSeconds = rateLimiter.getRemainingSeconds(userId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limit exceeded. Try again in " + waitSeconds + " seconds."));
        }

        log.info("Chat message received: {}", req.getMessage());
        AgentResponseDTO response = agentChatService.processQuery(req.getMessage());
        return ResponseEntity.ok(response);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "anonymous";
        }
        // Use principal's email or ID for stable rate-limit key
        Object principal = auth.getPrincipal();
        if (principal instanceof com.familie.cheltuieli_familie.model.User user) {
            return String.valueOf(user.getId());
        }
        return auth.getName();
    }
}
