package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.FamilyMemberDTO;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.FamilyRepository;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FamilyService {

    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyRepository       familyRepository;
    private final JwtUtil                jwtUtil;

    @Transactional
    public Map<String, Object> createFamily(String name, User requester) {
        if (!familyMemberRepository.findByUserId(requester.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ești deja membru al unei familii.");
        }

        Family family = new Family();
        family.setName(name != null && !name.isBlank() ? name.trim() : requester.getName() + "'s Family");
        family.setCreatedAt(LocalDate.now());
        familyRepository.save(family);

        FamilyMember member = new FamilyMember();
        member.setFamily(family);
        member.setUser(requester);
        member.setRole("Parent");
        familyMemberRepository.save(member);

        log.info("Familie nouă creată: '{}' (id={}) de către {}", family.getName(), family.getId(), requester.getEmail());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",   requester.getId());
        claims.put("role",     "Parent");
        claims.put("name",     requester.getName());
        claims.put("familyId", family.getId());
        String newToken = jwtUtil.generateToken(requester.getEmail(), claims);

        return Map.of("token", newToken, "role", "Parent", "familyId", family.getId());
    }

    public List<FamilyMemberDTO> getMembers(Long familyId, User requester) {
        verifyMembership(familyId, requester);
        return familyMemberRepository.findByFamilyId(familyId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public void deleteFamily(Long familyId, User requester) {
        FamilyMember membership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ești membru al acestei familii."));

        if (!isParentRole(membership.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doar un părinte poate șterge familia.");
        }

        List<FamilyMember> allMembers = familyMemberRepository.findByFamilyId(familyId);
        if (allMembers.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nu poți șterge familia dacă mai sunt și alți membri. Elimină-i mai întâi.");
        }

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Familia nu există."));

        familyMemberRepository.deleteAll(allMembers);
        familyRepository.delete(family);
        log.info("Familie ștearsă: id={} de către {}", familyId, requester.getEmail());
    }

    public void leaveFamily(Long familyId, User requester) {
        FamilyMember membership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nu ești membru al acestei familii."));

        boolean isLastParent = isParentRole(membership.getRole()) &&
                familyMemberRepository.findByFamilyId(familyId).stream()
                        .filter(m -> isParentRole(m.getRole()))
                        .count() == 1;
        if (isLastParent) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nu poți ieși din familie dacă ești singurul administrator. Șterge familia sau transferă rolul mai întâi.");
        }

        familyMemberRepository.delete(membership);
    }

    public FamilyMemberDTO updateMemberRole(Long familyId, Long memberId, String newRole, User requester) {
        FamilyMember requesterMembership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ești membru al acestei familii."));

        if (!"Parent".equalsIgnoreCase(requesterMembership.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doar administratorul poate schimba rolurile.");
        }

        FamilyMember member = familyMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membrul nu există."));

        if (!member.getFamily().getId().equals(familyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Membrul nu aparține acestei familii.");
        }

        if (member.getUser().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu îți poți schimba propriul rol.");
        }

        String normalized = normalizeRole(newRole);
        if (normalized.equals(normalizeRole(member.getRole()))) {
            return toDTO(member);
        }

        // guard: nu putem scoate ultimul Parent
        if (isParentRole(member.getRole()) && !isParentRole(normalized)) {
            long parentCount = familyMemberRepository.findByFamilyId(familyId).stream()
                    .filter(m -> isParentRole(m.getRole()))
                    .count();
            if (parentCount == 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Nu se poate retrograda singurul administrator. Numește mai întâi un alt administrator.");
            }
        }

        member.setRole(normalized);
        familyMemberRepository.save(member);
        log.info("Rol schimbat: membrul {} → {} de către {}", member.getUser().getEmail(), normalized, requester.getEmail());
        return toDTO(member);
    }

    public void removeMember(Long familyId, Long memberId, User requester) {
        verifyAdultRole(familyId, requester);

        FamilyMember member = familyMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membrul nu există."));

        if (!member.getFamily().getId().equals(familyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Membrul nu aparține acestei familii.");
        }

        boolean isLastParent = isParentRole(member.getRole()) &&
                familyMemberRepository.findByFamilyId(familyId).stream()
                        .filter(m -> isParentRole(m.getRole()))
                        .count() == 1;
        if (isLastParent) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nu se poate șterge ultimul administrator al familiei.");
        }

        familyMemberRepository.delete(member);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void verifyMembership(Long familyId, User requester) {
        if (!familyMemberRepository.existsByFamilyIdAndUserId(familyId, requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ești membru al acestei familii.");
        }
    }

    private void verifyAdultRole(Long familyId, User requester) {
        FamilyMember membership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ești membru al acestei familii."));

        if (!isParentRole(membership.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doar un părinte poate gestiona membrii familiei.");
        }
    }

    private boolean isParentRole(String role) {
        return "Parent".equalsIgnoreCase(role) || "Co-Parent".equalsIgnoreCase(role);
    }

    private FamilyMemberDTO toDTO(FamilyMember m) {
        return new FamilyMemberDTO(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getName(),
                m.getUser().getEmail(),
                normalizeRole(m.getRole())
        );
    }

    private String normalizeRole(String role) {
        if (role == null) return "Child";
        return switch (role.toLowerCase()) {
            case "parent"    -> "Parent";
            case "co-parent" -> "Co-Parent";
            default          -> "Child";
        };
    }
}
