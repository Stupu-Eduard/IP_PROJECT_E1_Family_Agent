package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.RagRequest;
import com.familie.cheltuieli_familie.service.RagRetrievalService;
import com.familie.cheltuieli_familie.model.User;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/rag")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RagQueryController {

    private final RagRetrievalService ragRetrievalService;

    @PostMapping("/query")
    public ResponseEntity<String> ragQuery(@Valid @RequestBody RagRequest request, Authentication authentication) {
        log.info("Received RAG query request: {}", request.getQuery());
        String identityBlock = buildIdentityBlock(authentication);
        String augmentedQuery = identityBlock + request.getQuery();
        String answer = ragRetrievalService.askWithContext(augmentedQuery);
        return ResponseEntity.ok(answer);
    }

    private String buildIdentityBlock(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return "";
        }
        return String.format("[IDENTITATE_AUTENTIFICATA: nume='%s', user_id=%d] ", user.getName(), user.getId());
    }
}
