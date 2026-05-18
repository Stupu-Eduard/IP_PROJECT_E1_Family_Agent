package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.FamilyMemberDTO;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.FamilyRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
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

    private static final String ROLE_PARENT    = "Parent";
    private static final String ROLE_CO_PARENT = "Co-Parent";
    private static final String ROLE_CHILD     = "Child";
    /**
     * Rol sentinel folosit pentru a marca o cerere de tranziție în așteptare.
     * Nu este expus utilizatorilor finali — este vizibil doar intern.
     */
    private static final String ROLE_PENDING_ADULT = "Child-PendingAdult";
    private static final String ERR_NOT_MEMBER = "Nu ești membru al acestei familii.";

    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyRepository       familyRepository;
    private final UserRepository         userRepository;
    private final JwtUtil                jwtUtil;
    private final com.familie.cheltuieli_familie.repository.ExpenseRepository expenseRepository;
    private final com.familie.cheltuieli_familie.repository.BudgetRepository budgetRepository;

    // ── Creare familie ────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> createFamily(String name, User requester) {
        if (!familyMemberRepository.findByUserId(requester.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ești deja membru al unei familii.");
        }

        Family family = new Family();
        family.setName(name != null && !name.isBlank() ? name.trim() : requester.getName() + "'s Family");
        family.setCreatedAt(LocalDate.now());
        Family savedFamily = familyRepository.save(family);

        FamilyMember member = new FamilyMember();
        member.setFamily(savedFamily);
        member.setUser(requester);
        member.setRole(ROLE_PARENT);
        familyMemberRepository.save(member);
        expenseRepository.linkUserExpensesToFamily(requester.getId(), savedFamily.getId());

        log.info("Familie nouă creată: '{}' (id={}) de către {}", savedFamily.getName(), savedFamily.getId(), requester.getEmail());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",   requester.getId());
        claims.put("role",     ROLE_PARENT);
        claims.put("name",     requester.getName());
        claims.put("familyId", savedFamily.getId());
        claims.put("familyName", savedFamily.getName());
        String newToken = jwtUtil.generateToken(requester.getEmail(), claims);

        return Map.of("token", newToken, "role", ROLE_PARENT, "familyId", savedFamily.getId());
    }

    // ── Citire membri ──────────────────────────────────────────────────────────

    public List<FamilyMemberDTO> getMembers(Long familyId, User requester) {
        verifyMembership(familyId, requester);
        return familyMemberRepository.findByFamilyId(familyId).stream()
                .map(this::toDTO)
                .toList();
    }

    // ── Ștergere familie ───────────────────────────────────────────────────────

    @Transactional
    public void deleteFamily(Long familyId, User requester) {
        FamilyMember membership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_NOT_MEMBER));

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

        expenseRepository.clearFamilyFromExpenses(familyId);
        budgetRepository.clearFamilyFromBudgets(familyId);
        familyMemberRepository.deleteAll(allMembers);
        familyRepository.delete(family);
        log.info("Familie ștearsă: id={} de către {}", familyId, requester.getEmail());
    }

    // ── Ieșire din familie ─────────────────────────────────────────────────────

    public void leaveFamily(Long familyId, User requester) {
        FamilyMember membership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ERR_NOT_MEMBER));

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

    // ── Schimbare rol ──────────────────────────────────────────────────────────

    public FamilyMemberDTO updateMemberRole(Long familyId, Long memberId, String newRole, User requester) {
        FamilyMember requesterMembership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_NOT_MEMBER));

        if (!ROLE_PARENT.equalsIgnoreCase(requesterMembership.getRole())) {
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

    // ── Eliminare membru ───────────────────────────────────────────────────────

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

    // ── FEATURE NOU: Ștergere cont copil de către adult ───────────────────────

    /**
     * Permite unui adult (Parent / Co-Parent) să șteargă contul unui copil din familie.
     * Elimină membrul din familie și șterge contul utilizatorului din baza de date.
     * Un adult nu poate șterge contul unui alt adult prin această metodă.
     */
    @Transactional
    public void deleteChildAccount(Long familyId, Long memberId, User requester) {
        verifyAdultRole(familyId, requester);

        FamilyMember member = familyMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membrul nu există."));

        if (!member.getFamily().getId().equals(familyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Membrul nu aparține acestei familii.");
        }

        if (isParentRole(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Nu poți șterge contul unui adult prin această operație. Folosește opțiunea de eliminare din familie.");
        }

        if (member.getUser().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nu îți poți șterge propriul cont prin această operație.");
        }

        User childUser = member.getUser();
        familyMemberRepository.delete(member);
        userRepository.delete(childUser);
        log.info("Cont copil șters: {} de către {}", childUser.getEmail(), requester.getEmail());
    }

    // ── FEATURE NOU: Solicitare tranziție adult (de la copil) ─────────────────

    /**
     * Un copil solicită tranziția la statut adult.
     * Rolul său din family_members devine "Child-PendingAdult" — un rol sentinel
     * care indică că există o cerere în așteptare pentru owner-ul familiei.
     * memberId trebuie să fie propriul family_member.id al copilului.
     */
    @Transactional
    public Map<String, Object> requestAdultTransition(Long familyId, Long memberId, User requester) {
        FamilyMember membership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_NOT_MEMBER));

        // Verificăm că memberId corespunde propriului cont
        if (!membership.getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Poți solicita tranziția doar pentru propriul cont.");
        }

        if (!ROLE_CHILD.equalsIgnoreCase(membership.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Doar un cont de tip Copil poate solicita tranziția la adult.");
        }

        membership.setRole(ROLE_PENDING_ADULT);
        familyMemberRepository.save(membership);
        log.info("Cerere tranziție adult: {} (memberId={})", requester.getEmail(), memberId);

        return Map.of(
                "message", "Cererea a fost trimisă. Vei primi un răspuns când proprietarul familiei o aprobă.",
                "status",  "pending"
        );
    }

    // ── FEATURE NOU: Aprobare / respingere cerere adult (de la owner) ─────────

    /**
     * Owner-ul familiei (Parent) aprobă sau respinge o cerere de tranziție adult.
     * Dacă aprobat → rolul devine "Co-Parent" și se emite un token reîmprospătat.
     * Dacă respins → rolul revine la "Child".
     */
    @Transactional
    public Map<String, Object> approveAdultTransition(Long familyId, Long memberId, boolean approve, User requester) {
        FamilyMember requesterMembership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_NOT_MEMBER));

        if (!ROLE_PARENT.equalsIgnoreCase(requesterMembership.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Doar proprietarul familiei (Parent) poate aproba tranziția la adult.");
        }

        FamilyMember member = familyMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membrul nu există."));

        if (!member.getFamily().getId().equals(familyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Membrul nu aparține acestei familii.");
        }

        if (!ROLE_PENDING_ADULT.equals(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Membrul nu are o cerere de tranziție în așteptare.");
        }

        String newRole = approve ? ROLE_CO_PARENT : ROLE_CHILD;
        member.setRole(newRole);
        familyMemberRepository.save(member);

        String action = approve ? "aprobată" : "respinsă";
        log.info("Cerere tranziție adult {}: membrul {} de către {}", action, member.getUser().getEmail(), requester.getEmail());

        // Dacă aprobat, emitem un token reîmprospătat pentru membrul promovat
        if (approve) {
            User promotedUser = member.getUser();
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId",   promotedUser.getId());
            claims.put("role",     ROLE_CO_PARENT);
            claims.put("name",     promotedUser.getName());
            claims.put("familyId", familyId);
            String newToken = jwtUtil.generateToken(promotedUser.getEmail(), claims);

            return Map.of(
                    "message",  "Cererea a fost aprobată. Membrul are acum statut Co-Parent.",
                    "approved", true,
                    "memberId", memberId,
                    "newToken", newToken   // trimis opțional — util dacă membrul e logat în aceeași sesiune
            );
        }

        return Map.of(
                "message",  "Cererea a fost respinsă. Membrul rămâne cu statut Copil.",
                "approved", false,
                "memberId", memberId
        );
    }

    // ── FEATURE NOU: Lista cereri tranziție adult ─────────────────────────────

    /**
     * Returnează lista membrilor cu cerere de tranziție în așteptare (rol = Child-PendingAdult).
     * Vizibil doar pentru owner (Parent).
     */
    public List<FamilyMemberDTO> getPendingAdultRequests(Long familyId, User requester) {
        FamilyMember requesterMembership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_NOT_MEMBER));

        if (!ROLE_PARENT.equalsIgnoreCase(requesterMembership.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Doar proprietarul familiei poate vedea cererile de tranziție.");
        }

        return familyMemberRepository.findByFamilyId(familyId).stream()
                .filter(m -> ROLE_PENDING_ADULT.equals(m.getRole()))
                .map(this::toDTO)
                .toList();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void verifyMembership(Long familyId, User requester) {
        if (!familyMemberRepository.existsByFamilyIdAndUserId(familyId, requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_NOT_MEMBER);
        }
    }

    private void verifyAdultRole(Long familyId, User requester) {
        FamilyMember membership = familyMemberRepository
                .findByFamilyIdAndUserId(familyId, requester.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, ERR_NOT_MEMBER));

        if (!isParentRole(membership.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doar un părinte poate gestiona membrii familiei.");
        }
    }

    private boolean isParentRole(String role) {
        return ROLE_PARENT.equalsIgnoreCase(role) || ROLE_CO_PARENT.equalsIgnoreCase(role);
    }

    private FamilyMemberDTO toDTO(FamilyMember m) {
        // Normalizăm rolul "Child-PendingAdult" la "Child" pentru vizibilitate frontend
        String displayRole = ROLE_PENDING_ADULT.equals(m.getRole()) ? ROLE_CHILD : normalizeRole(m.getRole());
        return new FamilyMemberDTO(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getName(),
                m.getUser().getEmail(),
                displayRole
        );
    }

    private String normalizeRole(String role) {
        if (role == null) return ROLE_CHILD;
        return switch (role.toLowerCase()) {
            case "parent"               -> ROLE_PARENT;
            case "co-parent"            -> ROLE_CO_PARENT;
            case "child-pendingadult"   -> ROLE_PENDING_ADULT;
            default                     -> ROLE_CHILD;
        };
    }
}