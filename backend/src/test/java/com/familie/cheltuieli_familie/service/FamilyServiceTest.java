package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.FamilyMemberDTO;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.FamilyRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FamilyServiceTest {

    private FamilyMemberRepository familyMemberRepository;
    private FamilyRepository familyRepository;
    private UserRepository userRepository;
    private JwtUtil jwtUtil;
    private FamilyService service;

    @BeforeEach
    void setUp() {
        familyMemberRepository = mock(FamilyMemberRepository.class);
        familyRepository = mock(FamilyRepository.class);
        userRepository = mock(UserRepository.class);
        jwtUtil = mock(JwtUtil.class);
        service = new FamilyService(familyMemberRepository, familyRepository, userRepository, jwtUtil);
    }

    private User mockUser(Long id, String name, String email) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        when(user.getEmail()).thenReturn(email);
        return user;
    }

    private Family mockFamily(Long id) {
        Family family = mock(Family.class);
        when(family.getId()).thenReturn(id);
        return family;
    }

    private FamilyMember mockMember(Long id, Family family, User user, String role) {
        FamilyMember fm = mock(FamilyMember.class);
        when(fm.getId()).thenReturn(id);
        when(fm.getFamily()).thenReturn(family);
        when(fm.getUser()).thenReturn(user);
        when(fm.getRole()).thenReturn(role);
        return fm;
    }

    // ── getMembers ───────────────────────────────────────────────────────────

    @Test
    void getMembers_memberOfFamily_returnsMappedList() {
        User requester = mockUser(1L, "Alex", "alex@test.com");
        Family family = mockFamily(10L);
        FamilyMember fm = mockMember(1L, family, requester, "Parent");

        when(familyMemberRepository.existsByFamilyIdAndUserId(10L, 1L)).thenReturn(true);
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(fm));

        List<FamilyMemberDTO> result = service.getMembers(10L, requester);

        assertEquals(1, result.size());
        assertEquals("Alex", result.get(0).getName());
        assertEquals("Parent", result.get(0).getRole());
    }

    @Test
    void getMembers_notMember_throwsForbidden() {
        User requester = mockUser(2L, "Bob", "bob@test.com");
        when(familyMemberRepository.existsByFamilyIdAndUserId(10L, 2L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getMembers(10L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getMembers_coParentRole_normalizesCorrectly() {
        User requester = mockUser(3L, "Maria", "maria@test.com");
        Family family = mockFamily(10L);
        FamilyMember fm = mockMember(1L, family, requester, "co-parent");

        when(familyMemberRepository.existsByFamilyIdAndUserId(10L, 3L)).thenReturn(true);
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(fm));

        List<FamilyMemberDTO> result = service.getMembers(10L, requester);

        assertEquals("Co-Parent", result.get(0).getRole());
    }

    @Test
    void getMembers_unknownRole_normalizesToChild() {
        User requester = mockUser(4L, "Ion", "ion@test.com");
        Family family = mockFamily(10L);
        FamilyMember fm = mockMember(1L, family, requester, "stranger");

        when(familyMemberRepository.existsByFamilyIdAndUserId(10L, 4L)).thenReturn(true);
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(fm));

        List<FamilyMemberDTO> result = service.getMembers(10L, requester);

        assertEquals("Child", result.get(0).getRole());
    }

    @Test
    void getMembers_pendingAdultRole_normalizesToChild() {
        User requester = mockUser(5L, "Andrei", "andrei@test.com");
        Family family = mockFamily(10L);
        FamilyMember fm = mockMember(2L, family, requester, "Child-PendingAdult");

        when(familyMemberRepository.existsByFamilyIdAndUserId(10L, 5L)).thenReturn(true);
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(fm));

        List<FamilyMemberDTO> result = service.getMembers(10L, requester);

        // Rolul sentinel trebuie normalizat la "Child" în DTO-ul returnat la frontend
        assertEquals("Child", result.get(0).getRole());
    }

    // ── leaveFamily ──────────────────────────────────────────────────────────

    @Test
    void leaveFamily_happyPath_deletesMembership() {
        User requester = mockUser(2L, "CoParent", "co@test.com");
        Family family = mockFamily(10L);
        FamilyMember requesterMembership = mockMember(2L, family, requester, "Co-Parent");
        FamilyMember anotherParent = mockMember(1L, family, mockUser(1L, "Parent", "p@test.com"), "Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 2L)).thenReturn(Optional.of(requesterMembership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(anotherParent, requesterMembership));

        service.leaveFamily(10L, requester);

        verify(familyMemberRepository).delete(requesterMembership);
    }

    @Test
    void leaveFamily_notMember_throwsNotFound() {
        User requester = mockUser(9L, "Stranger", "s@test.com");
        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 9L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.leaveFamily(10L, requester));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void leaveFamily_lastParent_throwsConflict() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        Family family = mockFamily(10L);
        FamilyMember membership = mockMember(1L, family, requester, "Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(membership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.leaveFamily(10L, requester));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void leaveFamily_child_leavesSuccessfully() {
        User requester = mockUser(5L, "Child", "child@test.com");
        Family family = mockFamily(10L);
        FamilyMember childMembership = mockMember(5L, family, requester, "Child");
        FamilyMember parent = mockMember(1L, family, mockUser(1L, "Parent", "p@test.com"), "Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 5L)).thenReturn(Optional.of(childMembership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(parent, childMembership));

        service.leaveFamily(10L, requester);

        verify(familyMemberRepository).delete(childMembership);
    }

    // ── createFamily ─────────────────────────────────────────────────────────

    @Test
    void createFamily_happyPath_returnsTokenAndRole() {
        User requester = mockUser(1L, "Alex", "alex@test.com");
        Family saved = mockFamily(10L);

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());
        when(familyRepository.save(any(Family.class))).thenReturn(saved);
        when(familyMemberRepository.save(any(FamilyMember.class))).thenReturn(mock(FamilyMember.class));
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("new-token");

        Map<String, Object> result = service.createFamily("Family Alex", requester);

        assertEquals("new-token", result.get("token"));
        assertEquals("Parent", result.get("role"));
        verify(familyRepository).save(any(Family.class));
        verify(familyMemberRepository).save(any(FamilyMember.class));
    }

    @Test
    void createFamily_alreadyInFamily_throwsConflict() {
        User requester = mockUser(1L, "Alex", "alex@test.com");
        Family family = mockFamily(10L);
        FamilyMember existing = mockMember(1L, family, requester, "Parent");

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createFamily("Test", requester));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ── deleteFamily ─────────────────────────────────────────────────────────

    @Test
    void deleteFamily_happyPath_deletesFamily() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        Family family = mockFamily(10L);
        FamilyMember membership = mockMember(1L, family, requester, "Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(membership));
        when(familyRepository.findById(10L)).thenReturn(Optional.of(family));

        service.deleteFamily(10L, requester);

        verify(familyMemberRepository).deleteAll(List.of(membership));
        verify(familyRepository).delete(family);
    }

    @Test
    void deleteFamily_notMember_throwsForbidden() {
        User requester = mockUser(9L, "Stranger", "s@test.com");
        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 9L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteFamily(10L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void deleteFamily_requesterIsChild_throwsForbidden() {
        User requester = mockUser(1L, "Child", "child@test.com");
        Family family = mockFamily(10L);
        FamilyMember membership = mockMember(1L, family, requester, "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteFamily(10L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void deleteFamily_multipleMembers_throwsConflict() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        Family family = mockFamily(10L);
        FamilyMember membership = mockMember(1L, family, requester, "Parent");
        FamilyMember other = mockMember(2L, family, mockUser(2L, "Other", "o@test.com"), "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(membership, other));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteFamily(10L, requester));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ── updateMemberRole ──────────────────────────────────────────────────────

    @Test
    void updateMemberRole_happyPath_changesRole() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        User target    = mockUser(5L, "Child",  "child@test.com");
        Family family  = mockFamily(10L);
        FamilyMember requesterMembership = mockMember(1L, family, requester, "Parent");
        FamilyMember targetMember        = mockMember(5L, family, target,    "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(requesterMembership));
        when(familyMemberRepository.findById(5L)).thenReturn(Optional.of(targetMember));
        when(familyMemberRepository.save(targetMember)).thenReturn(targetMember);

        FamilyMemberDTO result = service.updateMemberRole(10L, 5L, "Co-Parent", requester);

        verify(targetMember).setRole("Co-Parent");
        assertNotNull(result);
    }

    @Test
    void updateMemberRole_requesterNotMember_throwsForbidden() {
        User requester = mockUser(9L, "X", "x@test.com");
        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 9L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateMemberRole(10L, 5L, "Child", requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void updateMemberRole_requesterIsChild_throwsForbidden() {
        User requester = mockUser(1L, "Child", "child@test.com");
        Family family  = mockFamily(10L);
        FamilyMember membership = mockMember(1L, family, requester, "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateMemberRole(10L, 5L, "Parent", requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void updateMemberRole_memberNotFound_throwsNotFound() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        Family family  = mockFamily(10L);
        FamilyMember membership = mockMember(1L, family, requester, "Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(familyMemberRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateMemberRole(10L, 99L, "Child", requester));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateMemberRole_memberInDifferentFamily_throwsForbidden() {
        User requester  = mockUser(1L, "Parent", "parent@test.com");
        Family family10 = mockFamily(10L);
        Family family20 = mockFamily(20L);
        FamilyMember requesterMembership = mockMember(1L, family10, requester, "Parent");
        FamilyMember memberOther         = mockMember(8L, family20, mockUser(8L, "X", "x@t.com"), "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(requesterMembership));
        when(familyMemberRepository.findById(8L)).thenReturn(Optional.of(memberOther));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateMemberRole(10L, 8L, "Parent", requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void updateMemberRole_selfUpdate_throwsBadRequest() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        Family family  = mockFamily(10L);
        FamilyMember membership = mockMember(1L, family, requester, "Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(familyMemberRepository.findById(1L)).thenReturn(Optional.of(membership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateMemberRole(10L, 1L, "Child", requester));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateMemberRole_sameRole_returnsWithoutSave() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        User target    = mockUser(5L, "Child",  "child@test.com");
        Family family  = mockFamily(10L);
        FamilyMember requesterMembership = mockMember(1L, family, requester, "Parent");
        FamilyMember targetMember        = mockMember(5L, family, target,    "child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(requesterMembership));
        when(familyMemberRepository.findById(5L)).thenReturn(Optional.of(targetMember));

        service.updateMemberRole(10L, 5L, "Child", requester);

        verify(familyMemberRepository, never()).save(any());
    }

    @Test
    void updateMemberRole_lastParentDemotion_throwsConflict() {
        User requester  = mockUser(1L, "Parent",  "parent@test.com");
        User targetUser = mockUser(5L, "Target",  "target@test.com");
        Family family   = mockFamily(10L);
        FamilyMember requesterMembership = mockMember(1L, family, requester,  "Parent");
        FamilyMember targetMember        = mockMember(5L, family, targetUser, "Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(requesterMembership));
        when(familyMemberRepository.findById(5L)).thenReturn(Optional.of(targetMember));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(targetMember));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateMemberRole(10L, 5L, "Child", requester));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ── removeMember ─────────────────────────────────────────────────────────

    @Test
    void removeMember_happyPath_deletesChildMember() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        Family family = mockFamily(10L);
        FamilyMember requesterMembership = mockMember(1L, family, requester, "Parent");
        User childUser = mockUser(5L, "Child", "child@test.com");
        FamilyMember childMember = mockMember(5L, family, childUser, "Child");
        FamilyMember anotherParent = mockMember(2L, family, mockUser(2L, "P2", "p2@test.com"), "Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(requesterMembership));
        when(familyMemberRepository.findById(5L)).thenReturn(Optional.of(childMember));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(requesterMembership, anotherParent, childMember));

        service.removeMember(10L, 5L, requester);

        verify(familyMemberRepository).delete(childMember);
    }

    @Test
    void removeMember_requesterNotInFamily_throwsForbidden() {
        User requester = mockUser(2L, "Stranger", "s@test.com");
        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.removeMember(10L, 99L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void removeMember_requesterIsChild_throwsForbidden() {
        User requester = mockUser(3L, "Child", "child@test.com");
        Family family = mockFamily(10L);
        FamilyMember childMembership = mockMember(3L, family, requester, "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 3L)).thenReturn(Optional.of(childMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.removeMember(10L, 99L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void removeMember_memberNotFound_throwsNotFound() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        Family family = mockFamily(10L);
        FamilyMember membership = mockMember(1L, family, requester, "Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(familyMemberRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.removeMember(10L, 99L, requester));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void removeMember_memberInDifferentFamily_throwsForbidden() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        Family family10 = mockFamily(10L);
        Family family20 = mockFamily(20L);
        FamilyMember requesterMembership = mockMember(1L, family10, requester, "Parent");
        FamilyMember memberOfOther = mockMember(8L, family20, mockUser(8L, "Other", "o@test.com"), "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(requesterMembership));
        when(familyMemberRepository.findById(8L)).thenReturn(Optional.of(memberOfOther));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.removeMember(10L, 8L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void removeMember_lastParent_throwsConflict() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        Family family = mockFamily(10L);
        FamilyMember parentMembership = mockMember(1L, family, requester, "Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findById(1L)).thenReturn(Optional.of(parentMembership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(parentMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.removeMember(10L, 1L, requester));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ── deleteChildAccount ────────────────────────────────────────────────────

    @Test
    void deleteChildAccount_happyPath_deletesUserAndMember() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        User childUser = mockUser(5L, "Child", "child@test.com");
        Family family = mockFamily(10L);
        FamilyMember requesterMembership = mockMember(1L, family, requester, "Parent");
        FamilyMember childMember = mockMember(5L, family, childUser, "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(requesterMembership));
        when(familyMemberRepository.findById(5L)).thenReturn(Optional.of(childMember));

        service.deleteChildAccount(10L, 5L, requester);

        verify(familyMemberRepository).delete(childMember);
        verify(userRepository).delete(childUser);
    }

    @Test
    void deleteChildAccount_targetIsAdult_throwsForbidden() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        User adultUser = mockUser(6L, "CoParent", "co@test.com");
        Family family = mockFamily(10L);
        FamilyMember requesterMembership = mockMember(1L, family, requester, "Parent");
        FamilyMember adultMember = mockMember(6L, family, adultUser, "Co-Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(requesterMembership));
        when(familyMemberRepository.findById(6L)).thenReturn(Optional.of(adultMember));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteChildAccount(10L, 6L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void deleteChildAccount_requesterIsChild_throwsForbidden() {
        User requester = mockUser(3L, "Child", "child@test.com");
        Family family = mockFamily(10L);
        FamilyMember childMembership = mockMember(3L, family, requester, "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 3L)).thenReturn(Optional.of(childMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteChildAccount(10L, 99L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ── requestAdultTransition ────────────────────────────────────────────────

    @Test
    void requestAdultTransition_happyPath_setsPendingRole() {
        User requester = mockUser(5L, "Child", "child@test.com");
        Family family = mockFamily(10L);
        FamilyMember childMembership = mockMember(5L, family, requester, "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 5L)).thenReturn(Optional.of(childMembership));

        Map<String, Object> result = service.requestAdultTransition(10L, 5L, requester);

        verify(childMembership).setRole("Child-PendingAdult");
        verify(familyMemberRepository).save(childMembership);
        assertEquals("pending", result.get("status"));
    }

    @Test
    void requestAdultTransition_wrongMemberId_throwsForbidden() {
        User requester = mockUser(5L, "Child", "child@test.com");
        Family family = mockFamily(10L);
        // memberId al sesiunii este 5, dar se trimite 99 (al altui copil)
        FamilyMember childMembership = mockMember(5L, family, requester, "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 5L)).thenReturn(Optional.of(childMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.requestAdultTransition(10L, 99L, requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requestAdultTransition_requesterIsAdult_throwsBadRequest() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        Family family = mockFamily(10L);
        FamilyMember parentMembership = mockMember(1L, family, requester, "Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(parentMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.requestAdultTransition(10L, 1L, requester));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // ── approveAdultTransition ────────────────────────────────────────────────

    @Test
    void approveAdultTransition_approve_promotesToCoParent() {
        User owner = mockUser(1L, "Parent", "parent@test.com");
        User childUser = mockUser(5L, "Child", "child@test.com");
        Family family = mockFamily(10L);
        FamilyMember ownerMembership = mockMember(1L, family, owner, "Parent");
        FamilyMember childMembership = mockMember(5L, family, childUser, "Child-PendingAdult");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(familyMemberRepository.findById(5L)).thenReturn(Optional.of(childMembership));
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("new-child-token");

        Map<String, Object> result = service.approveAdultTransition(10L, 5L, true, owner);

        verify(childMembership).setRole("Co-Parent");
        verify(familyMemberRepository).save(childMembership);
        assertEquals(Boolean.TRUE, result.get("approved"));
    }

    @Test
    void approveAdultTransition_reject_revertsToChild() {
        User owner = mockUser(1L, "Parent", "parent@test.com");
        User childUser = mockUser(5L, "Child", "child@test.com");
        Family family = mockFamily(10L);
        FamilyMember ownerMembership = mockMember(1L, family, owner, "Parent");
        FamilyMember childMembership = mockMember(5L, family, childUser, "Child-PendingAdult");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(familyMemberRepository.findById(5L)).thenReturn(Optional.of(childMembership));

        Map<String, Object> result = service.approveAdultTransition(10L, 5L, false, owner);

        verify(childMembership).setRole("Child");
        assertEquals(Boolean.FALSE, result.get("approved"));
    }

    @Test
    void approveAdultTransition_requesterIsCoParent_throwsForbidden() {
        User coParent = mockUser(2L, "CoParent", "co@test.com");
        Family family = mockFamily(10L);
        FamilyMember coParentMembership = mockMember(2L, family, coParent, "Co-Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 2L)).thenReturn(Optional.of(coParentMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.approveAdultTransition(10L, 5L, true, coParent));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void approveAdultTransition_memberNotPending_throwsBadRequest() {
        User owner = mockUser(1L, "Parent", "parent@test.com");
        User childUser = mockUser(5L, "Child", "child@test.com");
        Family family = mockFamily(10L);
        FamilyMember ownerMembership = mockMember(1L, family, owner, "Parent");
        // Membrul nu are cerere în așteptare - rol normal "Child"
        FamilyMember childMembership = mockMember(5L, family, childUser, "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(familyMemberRepository.findById(5L)).thenReturn(Optional.of(childMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.approveAdultTransition(10L, 5L, true, owner));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // ── getPendingAdultRequests ───────────────────────────────────────────────

    @Test
    void getPendingAdultRequests_returnsOnlyPendingMembers() {
        User owner = mockUser(1L, "Parent", "parent@test.com");
        User childPending = mockUser(5L, "Pending", "pending@test.com");
        User childNormal = mockUser(6L, "Normal", "normal@test.com");
        Family family = mockFamily(10L);
        FamilyMember ownerMembership = mockMember(1L, family, owner, "Parent");
        FamilyMember pendingMember = mockMember(5L, family, childPending, "Child-PendingAdult");
        FamilyMember normalChild = mockMember(6L, family, childNormal, "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(ownerMembership, pendingMember, normalChild));

        List<FamilyMemberDTO> result = service.getPendingAdultRequests(10L, owner);

        assertEquals(1, result.size());
        assertEquals("Pending", result.get(0).getName());
    }
}