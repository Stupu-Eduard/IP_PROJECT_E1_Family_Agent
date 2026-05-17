package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.FamilyMemberDTO;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.BudgetRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.FamilyRepository;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private BudgetRepository budgetRepository;

    @InjectMocks
    private FamilyService familyService;

    private User requester;
    private Family family;
    private FamilyMember parentMembership;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId(1L);
        requester.setName("TestUser");
        requester.setEmail("test@example.com");

        family = new Family();
        family.setId(10L);
        family.setName("TestFamily");
        family.setCreatedAt(LocalDate.now());

        parentMembership = new FamilyMember();
        parentMembership.setId(100L);
        parentMembership.setUser(requester);
        parentMembership.setFamily(family);
        parentMembership.setRole("Parent");
    }

    // ── createFamily ────────────────────────────────────────────────────────

    @Test
    void createFamily_createsAndLinksExpenses() {
        when(familyMemberRepository.findByUserId(requester.getId())).thenReturn(Collections.emptyList());
        when(familyRepository.save(any(Family.class))).thenReturn(family);
        when(familyMemberRepository.save(any(FamilyMember.class))).thenReturn(parentMembership);
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("new-token");

        Map<String, Object> result = familyService.createFamily("TestFamily", requester);

        verify(expenseRepository).linkUserExpensesToFamily(requester.getId(), family.getId());
        assertEquals("new-token", result.get("token"));
        assertEquals("Parent", result.get("role"));
        assertEquals(10L, result.get("familyId"));
    }

    @Test
    void createFamily_usesDefaultNameWhenNullPassed() {
        when(familyMemberRepository.findByUserId(requester.getId())).thenReturn(Collections.emptyList());
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> {
            Family f = inv.getArgument(0);
            f.setId(10L);
            return f;
        });
        when(familyMemberRepository.save(any(FamilyMember.class))).thenReturn(parentMembership);
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("tok");

        familyService.createFamily(null, requester);

        verify(familyRepository).save(argThat(f -> f.getName().contains("TestUser")));
    }

    @Test
    void createFamily_throwsConflict_whenUserAlreadyInFamily() {
        when(familyMemberRepository.findByUserId(requester.getId())).thenReturn(List.of(parentMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.createFamily("X", requester));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ── deleteFamily ────────────────────────────────────────────────────────

    @Test
    void deleteFamily_clearsExpensesAndBudgetsBeforeDelete() {
        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findByFamilyId(family.getId()))
                .thenReturn(List.of(parentMembership));
        when(familyRepository.findById(family.getId())).thenReturn(Optional.of(family));

        familyService.deleteFamily(family.getId(), requester);

        verify(expenseRepository).clearFamilyFromExpenses(family.getId());
        verify(budgetRepository).clearFamilyFromBudgets(family.getId());
        verify(familyMemberRepository).deleteAll(List.of(parentMembership));
        verify(familyRepository).delete(family);
    }

    @Test
    void deleteFamily_throwsForbidden_whenNotMember() {
        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.deleteFamily(family.getId(), requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void deleteFamily_throwsForbidden_whenNotParent() {
        parentMembership.setRole("Child");
        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.deleteFamily(family.getId(), requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void deleteFamily_throwsConflict_whenOtherMembersExist() {
        User otherUser = new User();
        otherUser.setId(2L);
        FamilyMember other = new FamilyMember();
        other.setId(200L);
        other.setUser(otherUser);
        other.setFamily(family);
        other.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findByFamilyId(family.getId()))
                .thenReturn(List.of(parentMembership, other));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.deleteFamily(family.getId(), requester));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ── leaveFamily ──────────────────────────────────────────────────────────

    @Test
    void leaveFamily_removesNonLastParent() {
        User coParentUser = new User();
        coParentUser.setId(2L);
        FamilyMember coParent = new FamilyMember();
        coParent.setId(200L);
        coParent.setUser(coParentUser);
        coParent.setFamily(family);
        coParent.setRole("Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findByFamilyId(family.getId()))
                .thenReturn(List.of(parentMembership, coParent));

        familyService.leaveFamily(family.getId(), requester);

        verify(familyMemberRepository).delete(parentMembership);
    }

    @Test
    void leaveFamily_throwsConflict_whenLastParentTries() {
        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findByFamilyId(family.getId()))
                .thenReturn(List.of(parentMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.leaveFamily(family.getId(), requester));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void leaveFamily_throwsNotFound_whenNotMember() {
        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> familyService.leaveFamily(family.getId(), requester));
    }

    // ── getMembers ───────────────────────────────────────────────────────────

    @Test
    void getMembers_returnsListForMember() {
        when(familyMemberRepository.existsByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(true);
        when(familyMemberRepository.findByFamilyId(family.getId()))
                .thenReturn(List.of(parentMembership));

        List<FamilyMemberDTO> result = familyService.getMembers(family.getId(), requester);

        assertEquals(1, result.size());
        assertEquals(requester.getId(), result.get(0).getUserId());
    }

    @Test
    void getMembers_throwsForbidden_whenNotMember() {
        when(familyMemberRepository.existsByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.getMembers(family.getId(), requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ── updateMemberRole ─────────────────────────────────────────────────────

    @Test
    void updateMemberRole_changesChildToCoParent() {
        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setName("Target");
        targetUser.setEmail("target@test.com");

        FamilyMember targetMember = new FamilyMember();
        targetMember.setId(200L);
        targetMember.setUser(targetUser);
        targetMember.setFamily(family);
        targetMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(targetMember));
        when(familyMemberRepository.save(any())).thenReturn(targetMember);

        FamilyMemberDTO result = familyService.updateMemberRole(family.getId(), 200L, "co-parent", requester);

        assertEquals("Co-Parent", result.getRole());
    }

    @Test
    void updateMemberRole_returnsSameRole_whenRoleUnchanged() {
        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setName("Target");
        targetUser.setEmail("target@test.com");

        FamilyMember targetMember = new FamilyMember();
        targetMember.setId(200L);
        targetMember.setUser(targetUser);
        targetMember.setFamily(family);
        targetMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(targetMember));

        FamilyMemberDTO result = familyService.updateMemberRole(family.getId(), 200L, "child", requester);

        assertEquals("Child", result.getRole());
        verify(familyMemberRepository, never()).save(any());
    }

    @Test
    void updateMemberRole_throwsForbidden_whenRequesterNotParent() {
        parentMembership.setRole("Child");
        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.updateMemberRole(family.getId(), 200L, "parent", requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void updateMemberRole_throwsBadRequest_whenChangingOwnRole() {
        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(100L)).thenReturn(Optional.of(parentMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.updateMemberRole(family.getId(), 100L, "child", requester));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateMemberRole_throwsConflict_whenDemotingLastParent() {
        // Requester is Parent (passes admin check); findByFamilyId returns only the target
        // so parentCount == 1 and the guard fires CONFLICT
        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setName("Target");
        targetUser.setEmail("target@test.com");

        FamilyMember targetParent = new FamilyMember();
        targetParent.setId(200L);
        targetParent.setUser(targetUser);
        targetParent.setFamily(family);
        targetParent.setRole("Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(targetParent));
        // Simulate exactly 1 parent visible in the family roster
        when(familyMemberRepository.findByFamilyId(family.getId()))
                .thenReturn(List.of(targetParent));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.updateMemberRole(family.getId(), 200L, "child", requester));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void updateMemberRole_throwsForbidden_whenMemberBelongsToDifferentFamily() {
        Family otherFamily = new Family();
        otherFamily.setId(99L);

        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setName("Target");
        targetUser.setEmail("target@test.com");

        FamilyMember memberOfOtherFamily = new FamilyMember();
        memberOfOtherFamily.setId(200L);
        memberOfOtherFamily.setUser(targetUser);
        memberOfOtherFamily.setFamily(otherFamily);
        memberOfOtherFamily.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(memberOfOtherFamily));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.updateMemberRole(family.getId(), 200L, "parent", requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ── removeMember ─────────────────────────────────────────────────────────

    @Test
    void removeMember_removesChildMember() {
        User childUser = new User();
        childUser.setId(2L);
        childUser.setName("Child");
        childUser.setEmail("child@test.com");

        FamilyMember childMember = new FamilyMember();
        childMember.setId(200L);
        childMember.setUser(childUser);
        childMember.setFamily(family);
        childMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(childMember));

        familyService.removeMember(family.getId(), 200L, requester);

        verify(familyMemberRepository).delete(childMember);
    }

    @Test
    void removeMember_throwsConflict_whenRemovingLastParent() {
        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(100L)).thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findByFamilyId(family.getId()))
                .thenReturn(List.of(parentMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.removeMember(family.getId(), 100L, requester));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void removeMember_throwsForbidden_whenMemberBelongsToDifferentFamily() {
        Family otherFamily = new Family();
        otherFamily.setId(99L);

        User childUser = new User();
        childUser.setId(2L);

        FamilyMember memberOfOtherFamily = new FamilyMember();
        memberOfOtherFamily.setId(200L);
        memberOfOtherFamily.setUser(childUser);
        memberOfOtherFamily.setFamily(otherFamily);
        memberOfOtherFamily.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(memberOfOtherFamily));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.removeMember(family.getId(), 200L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
