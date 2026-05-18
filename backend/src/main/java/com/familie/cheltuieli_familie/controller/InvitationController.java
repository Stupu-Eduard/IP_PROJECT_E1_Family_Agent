package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.InvitationDTO;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping("/pending")
    public ResponseEntity<List<InvitationDTO>> getPending(Authentication auth) {
        return ResponseEntity.ok(invitationService.getPendingForUser(requester(auth)));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Map<String, Object>> accept(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(invitationService.accept(id, requester(auth)));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<Void> decline(
            @PathVariable Long id,
            Authentication auth) {
        invitationService.decline(id, requester(auth));
        return ResponseEntity.noContent().build();
    }

    private User requester(Authentication auth) {
        return (User) auth.getPrincipal();
    }
}
