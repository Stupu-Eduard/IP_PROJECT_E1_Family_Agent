package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.FamilyMemberDTO;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyMemberRepository familyMemberRepository;

    public List<FamilyMemberDTO> getMembers(Long familyId, User requester) {
        verifyMembership(familyId, requester);
        return familyMemberRepository.findByFamilyId(familyId).stream()
                .map(this::toDTO)
                .toList();
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
