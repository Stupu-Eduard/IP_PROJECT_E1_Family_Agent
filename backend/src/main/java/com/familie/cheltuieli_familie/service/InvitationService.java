package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.AddMemberRequest;
import com.familie.cheltuieli_familie.dto.InvitationDTO;
import com.familie.cheltuieli_familie.model.*;
import com.familie.cheltuieli_familie.repository.*;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private static final String STATUS_PENDING  = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_DECLINED = "DECLINED";

    private final FamilyInvitationRepository invitationRepository;
    private final FamilyMemberRepository     familyMemberRepository;
    private final FamilyRepository           familyRepository;
    private final UserRepository             userRepository;
    private final JwtUtil                    jwtUtil;

    public InvitationDTO createInvitation(Long familyId, AddMemberRequest request, User requester) {
        // verificăm că requestorul e adult în familie
        FamilyMember requesterMembership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ești membru al acestei familii."));
        if (!isParentRole(requesterMembership.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doar un părinte poate trimite invitații.");
        }

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Familia nu există."));

        // utilizatorul invitat trebuie să aibă cont
        User targetUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nu există niciun cont cu adresa " + request.getEmail()));

        if (familyMemberRepository.existsByFamilyIdAndUserId(familyId, targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Utilizatorul este deja membru al familiei.");
        }

        if (invitationRepository.existsByFamilyIdAndInviteeEmailAndStatus(familyId, request.getEmail(), STATUS_PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O invitație pentru acest email este deja în așteptare.");
        }

        FamilyInvitation inv = new FamilyInvitation();
        inv.setFamily(family);
        inv.setInviteeEmail(request.getEmail());
        inv.setRole(request.getRole());
        inv.setInvitedBy(requester);
        inv.setStatus(STATUS_PENDING);
        inv.setCreatedAt(LocalDateTime.now());
        invitationRepository.save(inv);

        log.info("Invitație trimisă de {} pentru {} în familia {}", requester.getEmail(), request.getEmail(), familyId);
        return toDTO(inv);
    }

    public List<InvitationDTO> getPendingForUser(User user) {
        return invitationRepository
                .findByInviteeEmailAndStatus(user.getEmail(), STATUS_PENDING)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public Map<String, Object> accept(Long invitationId, User user) {
        FamilyInvitation inv = findAndValidate(invitationId, user);

        FamilyMember member = new FamilyMember();
        member.setFamily(inv.getFamily());
        member.setUser(user);
        member.setRole(inv.getRole());
        familyMemberRepository.save(member);

        inv.setStatus(STATUS_ACCEPTED);
        invitationRepository.save(inv);

        log.info("{} a acceptat invitația în familia {}", user.getEmail(), inv.getFamily().getId());

        // generăm un token nou cu familyId inclus
        String role = normalizeRole(inv.getRole());
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",   user.getId());
        claims.put("role",     role);
        claims.put("name",     user.getName());
        claims.put("familyId", inv.getFamily().getId());
        String newToken = jwtUtil.generateToken(user.getEmail(), claims);

        return Map.of("token", newToken, "role", role);
    }

    public void decline(Long invitationId, User user) {
        FamilyInvitation inv = findAndValidate(invitationId, user);
        inv.setStatus(STATUS_DECLINED);
        invitationRepository.save(inv);
        log.info("{} a refuzat invitația în familia {}", user.getEmail(), inv.getFamily().getId());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private FamilyInvitation findAndValidate(Long id, User user) {
        FamilyInvitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitația nu există."));
        if (!inv.getInviteeEmail().equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invitația nu îți aparține.");
        }
        if (!STATUS_PENDING.equals(inv.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitația a fost deja procesată.");
        }
        return inv;
    }

    private boolean isParentRole(String role) {
        return "Parent".equalsIgnoreCase(role) || "Co-Parent".equalsIgnoreCase(role);
    }

    private String normalizeRole(String role) {
        if (role == null) return "Child";
        return switch (role.toLowerCase()) {
            case "parent"    -> "Parent";
            case "co-parent" -> "Co-Parent";
            default          -> "Child";
        };
    }

    private InvitationDTO toDTO(FamilyInvitation inv) {
        return new InvitationDTO(
                inv.getId(),
                inv.getFamily().getId(),
                inv.getFamily().getName(),
                inv.getInvitedBy().getName(),
                normalizeRole(inv.getRole())
        );
    }
}
