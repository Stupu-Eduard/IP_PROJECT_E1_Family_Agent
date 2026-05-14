package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.AddMemberRequest;
import com.familie.cheltuieli_familie.dto.InvitationDTO;
import com.familie.cheltuieli_familie.model.*;
import com.familie.cheltuieli_familie.repository.*;
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

class InvitationServiceTest {

    private FamilyInvitationRepository invitationRepository;
    private FamilyMemberRepository     familyMemberRepository;
    private FamilyRepository           familyRepository;
    private UserRepository             userRepository;
    private JwtUtil                    jwtUtil;
    private InvitationService          service;

    @BeforeEach
    void setUp() {
        invitationRepository   = mock(FamilyInvitationRepository.class);
        familyMemberRepository = mock(FamilyMemberRepository.class);
        familyRepository       = mock(FamilyRepository.class);
        userRepository         = mock(UserRepository.class);
        jwtUtil                = mock(JwtUtil.class);
        service = new InvitationService(invitationRepository, familyMemberRepository,
                familyRepository, userRepository, jwtUtil);
    }

    private User mockUser(Long id, String name, String email) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getName()).thenReturn(name);
        when(u.getEmail()).thenReturn(email);
        return u;
    }

    private Family mockFamily(Long id, String name) {
        Family f = mock(Family.class);
        when(f.getId()).thenReturn(id);
        when(f.getName()).thenReturn(name);
        return f;
    }

    private FamilyMember mockMember(Long id, Family family, User user, String role) {
        FamilyMember fm = mock(FamilyMember.class);
        when(fm.getId()).thenReturn(id);
        when(fm.getFamily()).thenReturn(family);
        when(fm.getUser()).thenReturn(user);
        when(fm.getRole()).thenReturn(role);
        return fm;
    }

    private FamilyInvitation mockInvitation(Long id, String email, String status, Family family, User invitedBy) {
        FamilyInvitation inv = mock(FamilyInvitation.class);
        when(inv.getId()).thenReturn(id);
        when(inv.getInviteeEmail()).thenReturn(email);
        when(inv.getStatus()).thenReturn(status);
        when(inv.getFamily()).thenReturn(family);
        when(inv.getInvitedBy()).thenReturn(invitedBy);
        when(inv.getRole()).thenReturn("Child");
        return inv;
    }

    private AddMemberRequest makeRequest(String email, String role) {
        AddMemberRequest req = new AddMemberRequest();
        req.setEmail(email);
        req.setRole(role);
        return req;
    }

    // ── createInvitation ─────────────────────────────────────────────────────

    @Test
    void createInvitation_happyPath_savesAndReturnsDTO() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        User target    = mockUser(2L, "Child",  "child@test.com");
        Family family  = mockFamily(10L, "Test Family");
        FamilyMember membership = mockMember(1L, family, requester, "Parent");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
        when(userRepository.findByEmail("child@test.com")).thenReturn(Optional.of(target));
        when(familyMemberRepository.findByUserId(2L)).thenReturn(List.of());
        when(invitationRepository.findByFamilyIdAndInviteeEmail(10L, "child@test.com")).thenReturn(Optional.empty());
        when(invitationRepository.save(any(FamilyInvitation.class))).thenAnswer(i -> i.getArgument(0));

        InvitationDTO result = service.createInvitation(10L, makeRequest("child@test.com", "Child"), requester);

        assertNotNull(result);
        verify(invitationRepository).save(any(FamilyInvitation.class));
    }

    @Test
    void createInvitation_requesterNotMember_throwsForbidden() {
        User requester = mockUser(1L, "X", "x@test.com");
        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createInvitation(10L, makeRequest("y@test.com", "Child"), requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void createInvitation_requesterIsChild_throwsForbidden() {
        User requester = mockUser(1L, "Child", "child@test.com");
        Family family  = mockFamily(10L, "Fam");
        FamilyMember childMembership = mockMember(1L, family, requester, "Child");
        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(childMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createInvitation(10L, makeRequest("other@test.com", "Child"), requester));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void createInvitation_familyNotFound_throwsNotFound() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        Family family  = mockFamily(10L, "Fam");
        FamilyMember membership = mockMember(1L, family, requester, "Parent");
        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(familyRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createInvitation(10L, makeRequest("x@test.com", "Child"), requester));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createInvitation_targetUserNotFound_throwsNotFound() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        Family family  = mockFamily(10L, "Fam");
        FamilyMember membership = mockMember(1L, family, requester, "Parent");
        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createInvitation(10L, makeRequest("notfound@test.com", "Child"), requester));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createInvitation_targetAlreadyInFamily_throwsConflict() {
        User requester     = mockUser(1L, "Parent", "parent@test.com");
        User target        = mockUser(2L, "Child",  "child@test.com");
        Family family      = mockFamily(10L, "Fam");
        Family otherFamily = mockFamily(20L, "Other");
        FamilyMember membership       = mockMember(1L, family,      requester, "Parent");
        FamilyMember targetMembership = mockMember(2L, otherFamily, target,    "Child");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
        when(userRepository.findByEmail("child@test.com")).thenReturn(Optional.of(target));
        when(familyMemberRepository.findByUserId(2L)).thenReturn(List.of(targetMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createInvitation(10L, makeRequest("child@test.com", "Child"), requester));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void createInvitation_pendingAlreadyExists_throwsConflict() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        User target    = mockUser(2L, "Child",  "child@test.com");
        Family family  = mockFamily(10L, "Fam");
        FamilyMember membership = mockMember(1L, family, requester, "Parent");
        FamilyInvitation existing = mockInvitation(99L, "child@test.com", "PENDING", family, requester);

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
        when(userRepository.findByEmail("child@test.com")).thenReturn(Optional.of(target));
        when(familyMemberRepository.findByUserId(2L)).thenReturn(List.of());
        when(invitationRepository.findByFamilyIdAndInviteeEmail(10L, "child@test.com")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createInvitation(10L, makeRequest("child@test.com", "Child"), requester));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void createInvitation_declinedExists_upserts() {
        User requester = mockUser(1L, "Parent", "parent@test.com");
        User target    = mockUser(2L, "Child",  "child@test.com");
        Family family  = mockFamily(10L, "Fam");
        FamilyMember membership = mockMember(1L, family, requester, "Parent");

        FamilyInvitation existing = new FamilyInvitation();
        existing.setStatus("DECLINED");

        when(familyMemberRepository.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
        when(userRepository.findByEmail("child@test.com")).thenReturn(Optional.of(target));
        when(familyMemberRepository.findByUserId(2L)).thenReturn(List.of());
        when(invitationRepository.findByFamilyIdAndInviteeEmail(10L, "child@test.com")).thenReturn(Optional.of(existing));
        when(invitationRepository.save(any())).thenReturn(existing);

        service.createInvitation(10L, makeRequest("child@test.com", "Child"), requester);

        verify(invitationRepository).save(existing);
        assertEquals("PENDING", existing.getStatus());
    }

    // ── getPendingForUser ─────────────────────────────────────────────────────

    @Test
    void getPendingForUser_returnsMappedList() {
        User user     = mockUser(1L, "Child",  "child@test.com");
        Family family = mockFamily(10L, "Family Name");
        User inviter  = mockUser(2L, "Parent", "parent@test.com");
        FamilyInvitation inv = mockInvitation(1L, "child@test.com", "PENDING", family, inviter);

        when(invitationRepository.findByInviteeEmailAndStatus("child@test.com", "PENDING"))
                .thenReturn(List.of(inv));

        List<InvitationDTO> result = service.getPendingForUser(user);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).familyId());
        assertEquals("Family Name", result.get(0).familyName());
        assertEquals("Parent", result.get(0).invitedByName());
    }

    @Test
    void getPendingForUser_noInvitations_returnsEmpty() {
        User user = mockUser(1L, "Child", "child@test.com");
        when(invitationRepository.findByInviteeEmailAndStatus("child@test.com", "PENDING"))
                .thenReturn(List.of());

        List<InvitationDTO> result = service.getPendingForUser(user);

        assertTrue(result.isEmpty());
    }

    // ── accept ────────────────────────────────────────────────────────────────

    @Test
    void accept_happyPath_returnsTokenAndRole() {
        User user     = mockUser(1L, "Child",  "child@test.com");
        Family family = mockFamily(10L, "Fam");
        User inviter  = mockUser(2L, "Parent", "parent@test.com");
        FamilyInvitation inv = mockInvitation(1L, "child@test.com", "PENDING", family, inviter);

        when(invitationRepository.findById(1L)).thenReturn(Optional.of(inv));
        when(invitationRepository.save(any())).thenReturn(inv);
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("new-token");

        Map<String, Object> result = service.accept(1L, user);

        assertEquals("new-token", result.get("token"));
        assertNotNull(result.get("role"));
        verify(familyMemberRepository).save(any(FamilyMember.class));
        verify(inv).setStatus("ACCEPTED");
    }

    @Test
    void accept_invitationNotFound_throwsNotFound() {
        User user = mockUser(1L, "Child", "child@test.com");
        when(invitationRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.accept(99L, user));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void accept_wrongUser_throwsForbidden() {
        User user     = mockUser(1L, "Child",  "child@test.com");
        Family family = mockFamily(10L, "Fam");
        User inviter  = mockUser(2L, "Parent", "parent@test.com");
        FamilyInvitation inv = mockInvitation(1L, "other@test.com", "PENDING", family, inviter);

        when(invitationRepository.findById(1L)).thenReturn(Optional.of(inv));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.accept(1L, user));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void accept_alreadyProcessed_throwsConflict() {
        User user     = mockUser(1L, "Child",  "child@test.com");
        Family family = mockFamily(10L, "Fam");
        User inviter  = mockUser(2L, "Parent", "parent@test.com");
        FamilyInvitation inv = mockInvitation(1L, "child@test.com", "ACCEPTED", family, inviter);

        when(invitationRepository.findById(1L)).thenReturn(Optional.of(inv));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.accept(1L, user));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ── decline ───────────────────────────────────────────────────────────────

    @Test
    void decline_happyPath_setsDeclined() {
        User user     = mockUser(1L, "Child",  "child@test.com");
        Family family = mockFamily(10L, "Fam");
        User inviter  = mockUser(2L, "Parent", "parent@test.com");
        FamilyInvitation inv = mockInvitation(1L, "child@test.com", "PENDING", family, inviter);

        when(invitationRepository.findById(1L)).thenReturn(Optional.of(inv));
        when(invitationRepository.save(any())).thenReturn(inv);

        service.decline(1L, user);

        verify(inv).setStatus("DECLINED");
        verify(invitationRepository).save(inv);
    }

    @Test
    void decline_wrongUser_throwsForbidden() {
        User user     = mockUser(1L, "Child",  "child@test.com");
        Family family = mockFamily(10L, "Fam");
        User inviter  = mockUser(2L, "Parent", "parent@test.com");
        FamilyInvitation inv = mockInvitation(1L, "other@test.com", "PENDING", family, inviter);

        when(invitationRepository.findById(1L)).thenReturn(Optional.of(inv));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.decline(1L, user));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void decline_alreadyProcessed_throwsConflict() {
        User user     = mockUser(1L, "Child",  "child@test.com");
        Family family = mockFamily(10L, "Fam");
        User inviter  = mockUser(2L, "Parent", "parent@test.com");
        FamilyInvitation inv = mockInvitation(1L, "child@test.com", "DECLINED", family, inviter);

        when(invitationRepository.findById(1L)).thenReturn(Optional.of(inv));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.decline(1L, user));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
