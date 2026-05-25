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

                // Verify that only children without markerColor got a color from the palette and were saved
                String[] palette = {
                        "#28b4f0", "#dc7c0e", "#11a811", "#F0E442", "#CC79A7",
                        "#8016a3", "#56B4E9", "#F7B801", "#000000", "#999999"
                };
                int paletteIdx = 0;
                for (int i = 0; i < children.size(); i++) {
                        FamilyMember child = children.get(i);
                        if (i % 2 == 0) {
                                // Should keep custom color
                                assertTrue(child.getMarkerColor().startsWith("#custom"));
                        } else {
                                // Should be assigned from palette, cycling if needed
                                String expected = palette[paletteIdx % palette.length];
                                assertEquals(expected, child.getMarkerColor());
                                paletteIdx++;
                                // Should be saved
                                verify(familyMemberRepository).save(child);
                        }
                }
        }

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

    @InjectMocks
    private FamilyService familyService;

    private Family family;
    private User requester;
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

    @Test
    void deleteFamily_shouldDeleteWhenLastParent() {
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
        FamilyMember member = new FamilyMember();
        member.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(member));

        assertDoesNotThrow(() -> familyService.leaveFamily(10L, requester));
        verify(familyMemberRepository).delete(member);
    }

    @Test
    void leaveFamily_shouldThrowWhenLastParent() {
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
        family.setOwnerId(3L);
        User target = createUser(2L, "target@test.com", "Target");

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
        FamilyMember member = new FamilyMember();
        member.setRole("Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(member));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> familyService.updateMemberRole(10L, 20L, "Parent", requester));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void removeMember_shouldRemoveMember() {
        family.setOwnerId(3L);
        User target = createUser(2L, "target@test.com", "Target");

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
        family.setOwnerId(2L);
        User target = createUser(2L, "target@test.com", "Target");

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
