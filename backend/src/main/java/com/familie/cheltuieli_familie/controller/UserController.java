package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.FamilyInvitationRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.FamilyRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.TokenBlacklistService;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"https://family-agent.me", "http://localhost:5173"})
public class UserController {

    private final ExpenseRepository       expenseRepository;
    private final UserRepository          userRepository;
    private final FamilyMemberRepository  familyMemberRepository;
    private final FamilyInvitationRepository familyInvitationRepository;
    private final FamilyRepository        familyRepository;
    private final TokenBlacklistService   blacklistService;
    private final JwtUtil                 jwtUtil;

    /** Lista globală de nume (folosită intern) */
    @GetMapping
    public List<String> list() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(User::getName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .toList();
    }

    /**
     * GET /api/v1/users/me
     * Returnează datele profilului utilizatorului curent.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication auth) {
        User user = requester(auth);
        return ResponseEntity.ok(Map.of(
                "id",    user.getId(),
                "name",  user.getName() != null ? user.getName() : "",
                "email", user.getEmail()
        ));
    }

    /**
     * PUT /api/v1/users/me
     * Actualizează numele utilizatorului curent.
     * Body: { "name": "Noul Nume" }
     * Returnează un token JWT reîmprospătat cu noul name în claims.
     */
    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody Map<String, String> body,
            Authentication auth,
            HttpServletRequest request) {

        User user = requester(auth);

        String newName = body != null ? body.get("name") : null;
        if (newName == null || newName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numele nu poate fi gol.");
        }
        newName = newName.trim();
        if (newName.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numele este prea lung (max 100 caractere).");
        }

        user.setName(newName);
        userRepository.save(user);
        log.info("Profil actualizat pentru utilizatorul {}", user.getEmail());

        // Blacklist token-ul curent și emite unul nou cu noul nume
        String oldToken = extractBearerToken(request);
        if (oldToken != null) {
            try {
                String jti = jwtUtil.extractJti(oldToken);
                java.util.Date exp = jwtUtil.extractExpiration(oldToken);
                blacklistService.revokeToken(jti, exp);
            } catch (Exception ignored) { /* token malformat, continuăm */ }
        }

        // Construim noul token cu claims actuale + noul nume
        // Rolul și familyId se citesc din membership-ul DB; dacă nu există membership
        // (utilizator fără familie), păstrăm rolul "Parent" ca default — consistent cu AuthController.
        List<FamilyMember> memberships = familyMemberRepository.findByUserId(user.getId());
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", user.getId());
        claims.put("name",   newName);
        if (!memberships.isEmpty()) {
            FamilyMember m = memberships.get(0);
            claims.put("role",     m.getRole());
            claims.put("familyId", m.getFamily().getId());
        } else {
            // Fără familie: rol default Parent (același comportament ca la login/refresh)
            claims.put("role", "Parent");
        }
        String newToken = jwtUtil.generateToken(user.getEmail(), claims);

        return ResponseEntity.ok(Map.of(
                "message", "Profilul a fost actualizat.",
                "token",   newToken,
                "name",    newName
        ));
    }

    /**
     * DELETE /api/v1/users/me
     * Șterge contul utilizatorului curent.
     * Doar adulții (Parent / Co-Parent) pot șterge propriul cont.
     * Un Parent nu poate șterge contul dacă este singurul administrator
     * dintr-o familie cu alți membri — trebuie să transfere rolul sau să șteargă mai întâi familia.
     */
    @Transactional
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteOwnAccount(
            Authentication auth,
            HttpServletRequest request) {

        User user = requester(auth);

        // Verificăm rolul din membership-ul DB — un Child nu poate șterge propriul cont.
        List<FamilyMember> memberships = familyMemberRepository.findByUserId(user.getId());
        boolean isChild = memberships.stream()
                .anyMatch(m -> "Child".equalsIgnoreCase(m.getRole())
                        || "Child-PendingAdult".equalsIgnoreCase(m.getRole()));
        if (isChild) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Conturile de tip Copil nu pot fi șterse de utilizator. Contactează un adult din familie.");
        }

        for (FamilyMember membership : memberships) {
            if ("Parent".equalsIgnoreCase(membership.getRole())) {
                long parentCount = familyMemberRepository.findByFamilyId(membership.getFamily().getId())
                        .stream()
                        .filter(m -> "Parent".equalsIgnoreCase(m.getRole()))
                        .count();
                if (parentCount == 1) {
                    long totalMembers = familyMemberRepository.findByFamilyId(membership.getFamily().getId()).size();
                    if (totalMembers > 1) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Ești singurul administrator al familiei. Transferă rolul sau șterge familia înainte de a-ți șterge contul.");
                    }
                    // Singur în familie ca Parent → ștergem și familia
                    familyMemberRepository.deleteAll(familyMemberRepository.findByFamilyId(membership.getFamily().getId()));
                    familyRepository.deleteById(membership.getFamily().getId());
                    continue;
                }
            }
            familyMemberRepository.delete(membership);
        }

        // Blacklist token curent
        String oldToken = extractBearerToken(request);
        if (oldToken != null) {
            try {
                blacklistService.revokeToken(jwtUtil.extractJti(oldToken), jwtUtil.extractExpiration(oldToken));
            } catch (Exception ignored) {}
        }

        // Stergem invitatiile trimise de acest user (invited_by)
        familyInvitationRepository.deleteByInvitedById(user.getId());

        // Decuplăm cheltuielile — user_id devine NULL (nu pierdem datele)
        expenseRepository.clearUserFromExpenses(user.getId());

        userRepository.delete(user);
        log.info("Cont șters: {}", user.getEmail());
        return ResponseEntity.noContent().build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User requester(Authentication auth) {
        return (User) auth.getPrincipal();
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}