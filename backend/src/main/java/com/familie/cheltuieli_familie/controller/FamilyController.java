package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.AddMemberRequest;
import com.familie.cheltuieli_familie.dto.FamilyMemberDTO;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.service.FamilyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/families")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "https://family-agent.me"})
public class FamilyController {

    private final FamilyService familyService;

    @GetMapping("/{familyId}/members")
    public ResponseEntity<List<FamilyMemberDTO>> getMembers(
            @PathVariable Long familyId,
            Authentication auth) {
        return ResponseEntity.ok(familyService.getMembers(familyId, requester(auth)));
    }

    @PostMapping("/{familyId}/members")
    public ResponseEntity<FamilyMemberDTO> addMember(
            @PathVariable Long familyId,
            @Valid @RequestBody AddMemberRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(familyService.addMember(familyId, request, requester(auth)));
    }

    @DeleteMapping("/{familyId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            Authentication auth) {
        familyService.removeMember(familyId, memberId, requester(auth));
        return ResponseEntity.noContent().build();
    }

    private User requester(Authentication auth) {
        return (User) auth.getPrincipal();
    }
}
