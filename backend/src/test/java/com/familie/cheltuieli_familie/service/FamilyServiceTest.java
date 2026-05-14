package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.FamilyMemberDTO;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.FamilyRepository;
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
    private JwtUtil jwtUtil;
    private FamilyService service;

    @BeforeEach
    void setUp() {
        familyMemberRepository = mock(FamilyMemberRepository.class);
        familyRepository = mock(FamilyRepository.class);
        jwtUtil = mock(JwtUtil.class);
        service = new FamilyService(familyMemberRepository, familyRepository, jwtUtil);
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
}
