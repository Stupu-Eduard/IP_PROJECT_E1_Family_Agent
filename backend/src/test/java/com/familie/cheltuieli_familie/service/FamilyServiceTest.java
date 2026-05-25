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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    // Am adăugat mock-ul lipsă pentru profileService
    @Mock
    private ProfileService profileService;

    @InjectMocks
    private FamilyService familyService;

    // ── shared fixtures ───────────────────────────────────────────────────────
    private User requester;
    private Family family;
    private FamilyMember parentMembership;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(10L);
        family.setOwnerId(1L);

        requester = createUser(1L, "test@test.com", "Test");

        parentMembership = new FamilyMember();
        parentMembership.setFamily(family);
        parentMembership.setUser(requester);
        parentMembership.setRole("Parent");
    }

    private User createUser(Long id, String email, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        return user;
    }

    // ── assignChildColorsIfMissing ───────────────────────────────────────────

    @Test
    void assignChildColorsIfMissing_assignsColorsAndCyclesPalette() {
        // Create 12 children, some with markerColor, some without
        List<FamilyMember> children = new java.util.ArrayList<>();
        for (int i = 0; i < 12; i++) {
            FamilyMember child = new FamilyMember();
            child.setId((long) (200 + i));
            child.setRole("Child");
            child.setFamily(family);
            child.setUser(new User());
            // Set markerColor for even indices only
            if (i % 2 == 0) {
                child.setMarkerColor("#custom" + i);
            }
            children.add(child);
        }

        // Spy the service to call real method
        FamilyService spyService = org.mockito.Mockito.spy(familyService);

        // Call the method under test
        spyService.assignChildColorsIfMissing(children);

        // Verify that only children without markerColor got a color from the palette and were saved, in sorted order by ID
        String[] palette = {
                "#28b4f0", "#dc7c0e", "#11a811", "#F0E442", "#CC79A7",
                "#8016a3", "#56B4E9", "#F7B801", "#000000", "#999999"
        };
        // Sort all children by ID to match service logic
        List<FamilyMember> sortedChildren = new java.util.ArrayList<>(children);
        sortedChildren.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        for (int i = 0; i < sortedChildren.size(); i++) {
            FamilyMember child = sortedChildren.get(i);
            String expectedColor = palette[i % palette.length];
            if ((child.getId() - 200) % 2 == 0) {
                // Even index: should keep custom color
                assertTrue(child.getMarkerColor().startsWith("#custom"));
            } else {
                // Odd index: should be assigned from palette
                assertEquals(expectedColor, child.getMarkerColor());
                verify(familyMemberRepository).save(child);
            }
        }
    }

    // ── createFamily ─────────────────────────────────────────────────────────

    @Test
    void createFamily_shouldCreateNewFamily() {
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
        FamilyMember member = new FamilyMember();
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(member));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.createFamily("New Family", requester));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void getMembers_shouldReturnMembers() {
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
        when(familyMemberRepository.existsByFamilyIdAndUserId(10L, 1L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.getMembers(10L, requester));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    // ── deleteFamily ──────────────────────────────────────────────────────────

    @Test
    void deleteFamily_shouldDeleteWhenLastParent() {
        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(parentMembership));
        when(familyRepository.findById(10L)).thenReturn(Optional.of(family));

        assertDoesNotThrow(() -> familyService.deleteFamily(10L, requester));
        verify(familyRepository).delete(family);
    }

    @Test
    void deleteFamily_shouldThrowWhenMultipleMembers() {
        FamilyMember member2 = new FamilyMember();
        member2.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(parentMembership, member2));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.deleteFamily(10L, requester));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    // ── leaveFamily ───────────────────────────────────────────────────────────

    @Test
    void leaveFamily_shouldAllowMemberToLeave() {
        FamilyMember childMember = new FamilyMember();
        childMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(childMember));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(childMember));

        assertDoesNotThrow(() -> familyService.leaveFamily(10L, requester));
        verify(familyMemberRepository).delete(childMember);
    }

    @Test
    void leaveFamily_shouldThrowWhenLastParent() {
        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(parentMembership));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.leaveFamily(10L, requester));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    // ── updateMemberRole ──────────────────────────────────────────────────────

    @Test
    void updateMemberRole_shouldUpdateRole() {
        User target = createUser(2L, "target@test.com", "Target");
        family.setOwnerId(3L);

        FamilyMember targetMember = new FamilyMember();
        targetMember.setId(20L);
        targetMember.setFamily(family);
        targetMember.setUser(target);
        targetMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(20L)).thenReturn(Optional.of(targetMember));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(parentMembership, targetMember));
        when(familyMemberRepository.save(any())).thenReturn(targetMember);

        FamilyMemberDTO result = familyService.updateMemberRole(10L, 20L, "Parent", requester);

        assertNotNull(result);
        assertEquals("Parent", result.getRole());
    }

    @Test
    void updateMemberRole_shouldThrowWhenNotParent() {
        FamilyMember childRequester = new FamilyMember();
        childRequester.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(childRequester));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.updateMemberRole(10L, 20L, "Parent", requester));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void updateMemberRole_throwsForbidden_whenChangingOwnerRole() {
        family.setOwnerId(2L);

        User ownerUser = createUser(2L, "owner@test.com", "Owner");
        FamilyMember ownerMember = new FamilyMember();
        ownerMember.setId(200L);
        ownerMember.setUser(ownerUser);
        ownerMember.setFamily(family);
        ownerMember.setRole("Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(ownerMember));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.updateMemberRole(family.getId(), 200L, "child", requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("fondatorului"));
    }

    @Test
    void updateMemberRole_changesChildToAdult() {
        User targetUser = createUser(2L, "target@test.com", "Target");

        FamilyMember targetMember = new FamilyMember();
        targetMember.setId(20L);
        targetMember.setFamily(family);
        targetMember.setUser(targetUser);
        targetMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(20L)).thenReturn(Optional.of(targetMember));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(parentMembership, targetMember));
        when(familyMemberRepository.save(any())).thenReturn(targetMember);

        FamilyMemberDTO result = familyService.updateMemberRole(family.getId(), 20L, "adult", requester);

        assertEquals("Adult", result.getRole());
    }

    @Test
    void updateMemberRole_withNullNewRole_treatsAsChild() {
        User targetUser = createUser(2L, "target@test.com", "Target");

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

    // ── removeMember ──────────────────────────────────────────────────────────

    @Test
    void removeMember_shouldRemoveMember() {
        User target = createUser(2L, "target@test.com", "Target");
        family.setOwnerId(3L);

        FamilyMember targetMember = new FamilyMember();
        targetMember.setFamily(family);
        targetMember.setUser(target);
        targetMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(20L)).thenReturn(Optional.of(targetMember));

        assertDoesNotThrow(() -> familyService.removeMember(10L, 20L, requester));
        verify(familyMemberRepository).delete(targetMember);
    }

    @Test
    void removeMember_shouldThrowWhenRemovingOwner() {
        User target = createUser(2L, "target@test.com", "Target");
        family.setOwnerId(2L);

        FamilyMember targetMember = new FamilyMember();
        targetMember.setFamily(family);
        targetMember.setUser(target);
        targetMember.setRole("Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(20L)).thenReturn(Optional.of(targetMember));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.removeMember(10L, 20L, requester));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void removeMember_throwsForbidden_whenRemovingOwner() {
        family.setOwnerId(2L);

        User ownerUser = createUser(2L, "owner@test.com", "Owner");
        FamilyMember ownerMember = new FamilyMember();
        ownerMember.setId(200L);
        ownerMember.setUser(ownerUser);
        ownerMember.setFamily(family);
        ownerMember.setRole("Co-Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(ownerMember));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.removeMember(family.getId(), 200L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Fondatorul"));
    }

    // ── deleteChildAccount ───────────────────────────────────────────────────

    @Test
    void deleteChildAccount_success_callsProfileServiceDeleteAccount() {
        User childUser = createUser(2L, "copil@test.com", "Copil");

        FamilyMember childMember = new FamilyMember();
        childMember.setId(20L);
        childMember.setUser(childUser);
        childMember.setFamily(family);
        childMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(20L)).thenReturn(Optional.of(childMember));

        // Corectat ca să apeleze deleteChildAccount în loc de removeMember
        assertDoesNotThrow(() -> familyService.deleteChildAccount(10L, 20L, requester));
        
        // Verificăm dacă serviciul de profile este apelat să șteargă copilul
        verify(profileService).deleteAccount(childUser);
    }

    @Test
    void deleteChildAccount_throwsForbidden_whenTargetIsParent() {
        User anotherParent = createUser(5L, "parent2@test.com", "Parent2");

        FamilyMember parentMember = new FamilyMember();
        parentMember.setId(300L);
        parentMember.setUser(anotherParent);
        parentMember.setFamily(family);
        parentMember.setRole("Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(300L)).thenReturn(Optional.of(parentMember));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.deleteChildAccount(family.getId(), 300L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(profileService, never()).deleteAccount(any());
    }

    @Test
    void deleteChildAccount_throwsBadRequest_whenRequesterDeletesOwnAccount() {
        FamilyMember selfMember = new FamilyMember();
        selfMember.setId(100L);
        selfMember.setUser(requester);
        selfMember.setFamily(family);
        selfMember.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(100L)).thenReturn(Optional.of(selfMember));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.deleteChildAccount(family.getId(), 100L, requester));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(profileService, never()).deleteAccount(any());
    }

    @Test
    void deleteChildAccount_throwsForbidden_whenMemberBelongsToDifferentFamily() {
        Family otherFamily = new Family();
        otherFamily.setId(99L);

        User childUser = createUser(3L, "child@test.com", "Child");

        FamilyMember memberOfOtherFamily = new FamilyMember();
        memberOfOtherFamily.setId(200L);
        memberOfOtherFamily.setUser(childUser);
        memberOfOtherFamily.setFamily(otherFamily);
        memberOfOtherFamily.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(family.getId(), requester.getId()))
                .thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(200L)).thenReturn(Optional.of(memberOfOtherFamily));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> familyService.deleteChildAccount(family.getId(), 200L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}