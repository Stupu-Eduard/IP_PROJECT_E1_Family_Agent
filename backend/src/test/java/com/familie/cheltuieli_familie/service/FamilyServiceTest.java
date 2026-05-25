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
import com.familie.cheltuieli_familie.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FamilyServiceTest {

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BudgetRepository budgetRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private ProfileService profileService;

    @InjectMocks
    private FamilyService familyService;

    private User createUser(Long id, String email, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        return user;
    }

    @Test
    void createFamily_shouldCreateNewFamily() {
        User requester = createUser(1L, "test@test.com", "Test");

        when(familyMemberRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> {
            Family f = inv.getArgument(0);
            f.setId(10L);
            return f;
        });
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("new_token");

        Map<String, Object> result = familyService.createFamily("My Family", requester);

        assertEquals("new_token", result.get("token"));
        assertEquals("Parent", result.get("role"));
        assertEquals(10L, result.get("familyId"));
    }

    @Test
    void createFamily_shouldThrowWhenAlreadyInFamily() {
        User requester = createUser(1L, "test@test.com", "Test");
        FamilyMember member = new FamilyMember();
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(member));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.createFamily("New Family", requester));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void getMembers_shouldReturnMembers() {
        User requester = createUser(1L, "test@test.com", "Test");
        Family family = new Family();
        family.setId(10L);
        family.setOwnerId(1L);

        User memberUser = createUser(2L, "member@test.com", "Member");
        FamilyMember member = new FamilyMember();
        member.setFamily(family);
        member.setUser(memberUser);
        member.setRole("Parent");

        when(familyMemberRepository.existsByFamilyIdAndUserId(10L, 1L)).thenReturn(true);
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(member));

        List<FamilyMemberDTO> result = familyService.getMembers(10L, requester);

        assertEquals(1, result.size());
        assertEquals("Member", result.get(0).getName());
    }

    @Test
    void getMembers_shouldThrowWhenNotMember() {
        User requester = createUser(1L, "test@test.com", "Test");
        when(familyMemberRepository.existsByFamilyIdAndUserId(10L, 1L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.getMembers(10L, requester));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void deleteFamily_shouldDeleteWhenLastParent() {
        User requester = createUser(1L, "test@test.com", "Test");
        Family family = new Family();
        family.setId(10L);

        FamilyMember member = new FamilyMember();
        member.setFamily(family);
        member.setUser(requester);
        member.setRole("Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(member));
        when(familyRepository.findById(10L)).thenReturn(Optional.of(family));

        assertDoesNotThrow(() -> familyService.deleteFamily(10L, requester));
        verify(familyRepository).delete(family);
    }

    @Test
    void deleteFamily_shouldThrowWhenMultipleMembers() {
        User requester = createUser(1L, "test@test.com", "Test");
        Family family = new Family();
        family.setId(10L);

        FamilyMember member1 = new FamilyMember();
        member1.setRole("Parent");
        FamilyMember member2 = new FamilyMember();
        member2.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(member1));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(member1, member2));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.deleteFamily(10L, requester));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void leaveFamily_shouldAllowMemberToLeave() {
        User requester = createUser(1L, "test@test.com", "Test");
        FamilyMember member = new FamilyMember();
        member.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(member));

        assertDoesNotThrow(() -> familyService.leaveFamily(10L, requester));
        verify(familyMemberRepository).delete(member);
    }

    @Test
    void leaveFamily_shouldThrowWhenLastParent() {
        User requester = createUser(1L, "test@test.com", "Test");
        FamilyMember member = new FamilyMember();
        member.setRole("Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(member));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.leaveFamily(10L, requester));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void updateMemberRole_shouldUpdateRole() {
        User requester = createUser(1L, "test@test.com", "Test");
        User target = createUser(2L, "target@test.com", "Target");

        Family family = new Family();
        family.setId(10L);
        family.setOwnerId(3L);

        FamilyMember requesterMember = new FamilyMember();
        requesterMember.setRole("Parent");

        FamilyMember targetMember = new FamilyMember();
        targetMember.setId(20L);
        targetMember.setFamily(family);
        targetMember.setUser(target);
        targetMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(requesterMember));
        when(familyMemberRepository.findById(20L)).thenReturn(Optional.of(targetMember));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(requesterMember, targetMember));
        when(familyMemberRepository.save(any())).thenReturn(targetMember);

        FamilyMemberDTO result = familyService.updateMemberRole(10L, 20L, "Parent", requester);

        assertNotNull(result);
        assertEquals("Parent", result.getRole());
    }

    @Test
    void updateMemberRole_shouldThrowWhenNotParent() {
        User requester = createUser(1L, "test@test.com", "Test");
        FamilyMember member = new FamilyMember();
        member.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.updateMemberRole(10L, 20L, "Parent", requester));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void removeMember_shouldRemoveMember() {
        User requester = createUser(1L, "test@test.com", "Test");
        User target = createUser(2L, "target@test.com", "Target");

        Family family = new Family();
        family.setId(10L);
        family.setOwnerId(3L);

        FamilyMember requesterMember = new FamilyMember();
        requesterMember.setRole("Parent");

        FamilyMember targetMember = new FamilyMember();
        targetMember.setFamily(family);
        targetMember.setUser(target);
        targetMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(requesterMember));
        when(familyMemberRepository.findById(20L)).thenReturn(Optional.of(targetMember));

        assertDoesNotThrow(() -> familyService.removeMember(10L, 20L, requester));
        verify(familyMemberRepository).delete(targetMember);
    }

    @Test
    void removeMember_shouldThrowWhenRemovingOwner() {
        User requester = createUser(1L, "test@test.com", "Test");
        User target = createUser(2L, "target@test.com", "Target");

        Family family = new Family();
        family.setId(10L);
        family.setOwnerId(2L);

        FamilyMember requesterMember = new FamilyMember();
        requesterMember.setRole("Parent");

        FamilyMember targetMember = new FamilyMember();
        targetMember.setFamily(family);
        targetMember.setUser(target);
        targetMember.setRole("Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(requesterMember));
        when(familyMemberRepository.findById(20L)).thenReturn(Optional.of(targetMember));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.removeMember(10L, 20L, requester));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    // ── deleteChildAccount ───────────────────────────────────────────────────

    @Test
    void deleteChildAccount_success_callsProfileServiceDeleteAccount() {
        User childUser = new User();
        childUser.setId(2L);
        childUser.setName("Copil");
        childUser.setEmail("copil@test.com");

        FamilyMember childMember = new FamilyMember();
        childMember.setId(200L);
        childMember.setUser(childUser);
        childMember.setFamily(family);
        childMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(childMember));

        familyService.deleteChildAccount(family.getId(), 200L, requester);

        verify(profileService).deleteAccount(childUser);
    }

    @Test
    void deleteChildAccount_throwsForbidden_whenRequesterIsChild() {
        FamilyMember childRequesterMembership = new FamilyMember();
        childRequesterMembership.setId(99L);
        childRequesterMembership.setUser(requester);
        childRequesterMembership.setFamily(family);
        childRequesterMembership.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(childRequesterMembership));

        Long famId = family.getId();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.deleteChildAccount(famId, 200L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(profileService, never()).deleteAccount(any());
    }

    @Test
    void deleteChildAccount_throwsForbidden_whenTargetIsParent() {
        User anotherParent = new User();
        anotherParent.setId(5L);
        anotherParent.setEmail("parent2@test.com");

        FamilyMember parentMember = new FamilyMember();
        parentMember.setId(300L);
        parentMember.setUser(anotherParent);
        parentMember.setFamily(family);
        parentMember.setRole("Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(300L)).thenReturn(Optional.of(parentMember));

        Long famId = family.getId();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.deleteChildAccount(famId, 300L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(profileService, never()).deleteAccount(any());
    }

    @Test
    void deleteChildAccount_throwsBadRequest_whenRequesterDeletesOwnAccount() {
        // Membrul tinta este chiar requesterul (child member cu acelasi userId)
        FamilyMember selfMember = new FamilyMember();
        selfMember.setId(100L);
        selfMember.setUser(requester);   // acelasi user
        selfMember.setFamily(family);
        selfMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(100L)).thenReturn(Optional.of(selfMember));

        Long famId = family.getId();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.deleteChildAccount(famId, 100L, requester));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(profileService, never()).deleteAccount(any());
    }

    @Test
    void deleteChildAccount_throwsForbidden_whenMemberBelongsToDifferentFamily() {
        Family otherFamily = new Family();
        otherFamily.setId(99L);

        User childUser = new User();
        childUser.setId(3L);

        FamilyMember memberOfOtherFamily = new FamilyMember();
        memberOfOtherFamily.setId(200L);
        memberOfOtherFamily.setUser(childUser);
        memberOfOtherFamily.setFamily(otherFamily);
        memberOfOtherFamily.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(memberOfOtherFamily));

        Long famId = family.getId();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.deleteChildAccount(famId, 200L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void removeMember_throwsForbidden_whenRemovingOwner() {
        family.setOwnerId(2L);

        User ownerUser = new User();
        ownerUser.setId(2L);
        ownerUser.setName("Owner");
        ownerUser.setEmail("owner@test.com");

        FamilyMember ownerMember = new FamilyMember();
        ownerMember.setId(200L);
        ownerMember.setUser(ownerUser);
        ownerMember.setFamily(family);
        ownerMember.setRole("Co-Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(ownerMember));

        Long famId = family.getId();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.removeMember(famId, 200L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Fondatorul"));
    }

    // ── updateMemberRole owner/normalizeRole coverage ────────────────────────

    @Test
    void updateMemberRole_throwsForbidden_whenChangingOwnerRole() {
        family.setOwnerId(2L);

        User ownerUser = new User();
        ownerUser.setId(2L);
        ownerUser.setName("Owner");
        ownerUser.setEmail("owner@test.com");

        FamilyMember ownerMember = new FamilyMember();
        ownerMember.setId(200L);
        ownerMember.setUser(ownerUser);
        ownerMember.setFamily(family);
        ownerMember.setRole("Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(ownerMember));

        Long famId = family.getId();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.updateMemberRole(famId, 200L, "child", requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("fondatorului"));
    }

    @Test
    void updateMemberRole_changesChildToAdult() {
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

        FamilyMemberDTO result = familyService.updateMemberRole(family.getId(), 200L, "adult", requester);

        assertEquals("Adult", result.getRole());
    }

    @Test
    void updateMemberRole_withNullNewRole_treatsAsChild() {
        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setName("Target");
        targetUser.setEmail("target@test.com");

        FamilyMember targetMember = new FamilyMember();
        targetMember.setId(200L);
        targetMember.setUser(targetUser);
        targetMember.setFamily(family);
        targetMember.setRole("Adult");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(targetMember));
        when(familyMemberRepository.save(any())).thenReturn(targetMember);

        FamilyMemberDTO result = familyService.updateMemberRole(family.getId(), 200L, null, requester);

        assertEquals("Child", result.getRole());
        verify(familyMemberRepository).save(any());
    }

}