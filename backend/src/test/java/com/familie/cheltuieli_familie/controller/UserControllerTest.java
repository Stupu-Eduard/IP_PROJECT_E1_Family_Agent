package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.FamilyRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.TokenBlacklistService;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserRepository userRepository;
    private FamilyMemberRepository familyMemberRepository;
    private FamilyRepository familyRepository;
    private TokenBlacklistService blacklistService;
    private JwtUtil jwtUtil;
    private UserController controller;

    private Authentication auth;
    private User currentUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        familyMemberRepository = mock(FamilyMemberRepository.class);
        familyRepository = mock(FamilyRepository.class);
        blacklistService = mock(TokenBlacklistService.class);
        jwtUtil = mock(JwtUtil.class);

        controller = new UserController(
                userRepository, familyMemberRepository, familyRepository,
                blacklistService, jwtUtil
        );

        currentUser = mock(User.class);
        when(currentUser.getId()).thenReturn(1L);
        when(currentUser.getName()).thenReturn("Ion Popescu");
        when(currentUser.getEmail()).thenReturn("ion@test.com");

        auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(currentUser);
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_filtersOutNullAndBlankNames() {
        User u1 = new User(); u1.setName("Ana");
        User u2 = new User(); u2.setName("");
        User u3 = new User(); u3.setName(null);

        when(userRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
                .thenReturn(List.of(u1, u2, u3));

        List<String> result = controller.list();

        assertEquals(List.of("Ana"), result);
        verify(userRepository).findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    // ── getProfile ───────────────────────────────────────────────────────────

    @Test
    void getProfile_returnsUserData() {
        ResponseEntity<Map<String, Object>> response = controller.getProfile(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().get("id"));
        assertEquals("Ion Popescu", response.getBody().get("name"));
        assertEquals("ion@test.com", response.getBody().get("email"));
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    void updateProfile_validName_savesAndReturnsNewToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer old-token");
        when(jwtUtil.extractJti("old-token")).thenReturn("jti-123");
        when(jwtUtil.extractExpiration("old-token")).thenReturn(new Date());
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("new-token");

        ResponseEntity<Map<String, Object>> response =
                controller.updateProfile(Map.of("name", "Noul Nume"), auth, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("new-token", response.getBody().get("token"));
        assertEquals("Noul Nume", response.getBody().get("name"));
        verify(currentUser).setName("Noul Nume");
        verify(userRepository).save(currentUser);
        verify(blacklistService).revokeToken(eq("jti-123"), any(Date.class));
    }

    @Test
    void updateProfile_emptyName_throwsBadRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.updateProfile(Map.of("name", "   "), auth, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_nullBody_throwsBadRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.updateProfile(null, auth, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateProfile_nameTooLong_throwsBadRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        String longName = "A".repeat(101);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.updateProfile(Map.of("name", longName), auth, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void updateProfile_noMembership_tokenIncludesDefaultParentRole() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer t");
        when(jwtUtil.extractJti("t")).thenReturn("jti-1");
        when(jwtUtil.extractExpiration("t")).thenReturn(new Date());
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("new-tok");

        ResponseEntity<Map<String, Object>> response =
                controller.updateProfile(Map.of("name", "Test User"), auth, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(jwtUtil).generateToken(eq("ion@test.com"), argThat(claims ->
                "Parent".equals(claims.get("role"))));
    }

    // ── deleteOwnAccount ─────────────────────────────────────────────────────

    @Test
    void deleteOwnAccount_noFamily_deletesUser() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer old-token");
        when(jwtUtil.extractJti("old-token")).thenReturn("jti-abc");
        when(jwtUtil.extractExpiration("old-token")).thenReturn(new Date());
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());

        ResponseEntity<Void> response = controller.deleteOwnAccount(auth, request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userRepository).delete(currentUser);
        verify(blacklistService).revokeToken(eq("jti-abc"), any(Date.class));
    }

    @Test
    void deleteOwnAccount_childRole_throwsForbidden() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        Family family = mock(Family.class);
        when(family.getId()).thenReturn(10L);

        FamilyMember childMembership = mock(FamilyMember.class);
        when(childMembership.getRole()).thenReturn("Child");
        when(childMembership.getFamily()).thenReturn(family);

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(childMembership));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.deleteOwnAccount(auth, request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteOwnAccount_soleParentWithOtherMembers_throwsConflict() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        Family family = mock(Family.class);
        when(family.getId()).thenReturn(10L);

        FamilyMember myMembership = mock(FamilyMember.class);
        when(myMembership.getRole()).thenReturn("Parent");
        when(myMembership.getFamily()).thenReturn(family);

        FamilyMember otherMember = mock(FamilyMember.class);
        when(otherMember.getRole()).thenReturn("Child");

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(myMembership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(myMembership, otherMember));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.deleteOwnAccount(auth, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteOwnAccount_soleParentAloneInFamily_deletesUserAndFamily() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer t");
        when(jwtUtil.extractJti("t")).thenReturn("jti-x");
        when(jwtUtil.extractExpiration("t")).thenReturn(new Date());

        Family family = mock(Family.class);
        when(family.getId()).thenReturn(10L);

        FamilyMember myMembership = mock(FamilyMember.class);
        when(myMembership.getRole()).thenReturn("Parent");
        when(myMembership.getFamily()).thenReturn(family);

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(myMembership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(myMembership));

        controller.deleteOwnAccount(auth, request);

        verify(familyMemberRepository).deleteAll(List.of(myMembership));
        verify(familyRepository).deleteById(10L);
        verify(userRepository).delete(currentUser);
    }
}