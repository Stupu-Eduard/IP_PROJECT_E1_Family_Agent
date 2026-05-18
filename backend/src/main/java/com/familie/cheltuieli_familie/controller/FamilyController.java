package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.AddMemberRequest;
import com.familie.cheltuieli_familie.dto.FamilyMemberDTO;
import com.familie.cheltuieli_familie.dto.InvitationDTO;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.service.FamilyService;
import com.familie.cheltuieli_familie.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService      familyService;
    private final InvitationService  invitationService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createFamily(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String name = body != null ? body.get("name") : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(familyService.createFamily(name, requester(auth)));
    }

    @GetMapping("/{familyId}/members")
    public ResponseEntity<List<FamilyMemberDTO>> getMembers(
            @PathVariable Long familyId,
            Authentication auth) {
        return ResponseEntity.ok(familyService.getMembers(familyId, requester(auth)));
    }

    @PostMapping("/{familyId}/members")
    public ResponseEntity<InvitationDTO> inviteMember(
            @PathVariable Long familyId,
            @Valid @RequestBody AddMemberRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.createInvitation(familyId, request, requester(auth)));
    }

    @PatchMapping("/{familyId}/members/{memberId}/role")
    public ResponseEntity<FamilyMemberDTO> updateMemberRole(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String newRole = body != null ? body.get("role") : null;
        return ResponseEntity.ok(familyService.updateMemberRole(familyId, memberId, newRole, requester(auth)));
    }

    @DeleteMapping("/{familyId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            Authentication auth) {
        familyService.removeMember(familyId, memberId, requester(auth));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{familyId}")
    public ResponseEntity<Void> deleteFamily(
            @PathVariable Long familyId,
            Authentication auth) {
        familyService.deleteFamily(familyId, requester(auth));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{familyId}/leave")
    public ResponseEntity<Void> leaveFamily(
            @PathVariable Long familyId,
            Authentication auth) {
        familyService.leaveFamily(familyId, requester(auth));
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/v1/families/{familyId}/members/{memberId}/account
     *
     * Permite unui adult (Parent / Co-Parent) să șteargă contul unui copil din familie.
     * Elimină membrul din familie și șterge contul utilizatorului.
     */
    @DeleteMapping("/{familyId}/members/{memberId}/account")
    public ResponseEntity<Void> deleteChildAccount(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            Authentication auth) {
        familyService.deleteChildAccount(familyId, memberId, requester(auth));
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/families/{familyId}/members/{memberId}/request-adult
     *
     * Un copil (Child) solicită tranziția la statut adult (Co-Parent).
     * Creează o notificare/cerere care va fi vizibilă owner-ului familiei (Parent).
     * memberId = family_members.id al copilului care face cererea (trebuie să fie propriul cont).
     */
    @PostMapping("/{familyId}/members/{memberId}/request-adult")
    public ResponseEntity<Map<String, Object>> requestAdultTransition(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            Authentication auth) {
        return ResponseEntity.ok(familyService.requestAdultTransition(familyId, memberId, requester(auth)));
    }

    /**
     * POST /api/v1/families/{familyId}/members/{memberId}/approve-adult
     *
     * Owner-ul familiei (Parent) aprobă sau respinge o cerere de tranziție adult.
     * Body: { "approve": true/false }
     * Dacă aprobat, rolul copilului devine "Co-Parent" și se emite un token reîmprospătat.
     */
    @PostMapping("/{familyId}/members/{memberId}/approve-adult")
    public ResponseEntity<Map<String, Object>> approveAdultTransition(
            @PathVariable Long familyId,
            @PathVariable Long memberId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        boolean approve = Boolean.TRUE.equals(body != null ? body.get("approve") : null);
        return ResponseEntity.ok(familyService.approveAdultTransition(familyId, memberId, approve, requester(auth)));
    }

    /**
     * GET /api/v1/families/{familyId}/adult-requests
     *
     * Returnează lista cererilor de tranziție adult în așteptare (pentru owner).
     */
    @GetMapping("/{familyId}/adult-requests")
    public ResponseEntity<List<FamilyMemberDTO>> getPendingAdultRequests(
            @PathVariable Long familyId,
            Authentication auth) {
        return ResponseEntity.ok(familyService.getPendingAdultRequests(familyId, requester(auth)));
    }

    private User requester(Authentication auth) {
        return (User) auth.getPrincipal();
    }
}