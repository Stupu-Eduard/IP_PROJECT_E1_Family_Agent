package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.LoginRequest;
import com.familie.cheltuieli_familie.dto.RegisterRequest;
import com.familie.cheltuieli_familie.dto.ForgotPasswordRequest;
import com.familie.cheltuieli_familie.dto.ResetPasswordRequest;
import com.familie.cheltuieli_familie.dto.SecurityQuestion;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.Answer;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.FamilyRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.repository.AnswerRepository;
import com.familie.cheltuieli_familie.security.service.TokenBlacklistService;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AnswerRepository answerRepository;


    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService blacklistService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void login_CandDateSuntCorecte_ReturneazaToken() {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setEmail("test@familie.com");
        request.setPassword("parolaBuna");

        User user = new User();
        user.setId(1L);
        user.setName("Edi");
        user.setEmail("test@familie.com");
        user.setPasswordH("parolaBuna");

        when(userRepository.findByEmail("test@familie.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parolaBuna", "parolaBuna")).thenReturn(true);
        when(familyMemberRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(jwtUtil.generateToken(eq("test@familie.com"), any())).thenReturn("mock-token");

        // WHEN
        ResponseEntity<Object> result = authController.login(request);

        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertNotNull(body);
        assertEquals("mock-token", body.get("token"));
        assertEquals("Edi", body.get("userName"));
        assertEquals("Parent", body.get("role"));
    }

    @Test
    void login_CandUserAreRolCopil_NormalizeazaRol() {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setEmail("copil@familie.com");
        request.setPassword("parola");

        User user = new User();
        user.setId(2L);
        user.setName("Copil");
        user.setEmail("copil@familie.com");
        user.setPasswordH("parola");

        FamilyMember member = new FamilyMember();
        member.setRole("child"); // lowercase in DB
        com.familie.cheltuieli_familie.model.Family family = new com.familie.cheltuieli_familie.model.Family();
        family.setId(10L);
        member.setFamily(family);

        when(userRepository.findByEmail("copil@familie.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parola", "parola")).thenReturn(true);
        when(familyMemberRepository.findByUserId(2L)).thenReturn(List.of(member));
        when(jwtUtil.generateToken(any(), any())).thenReturn("tk");

        // WHEN
        ResponseEntity<Object> result = authController.login(request);

        // THEN
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("Child", body.get("role")); // Capitalized
    }

    @Test
    void login_CandMembruAreFamilieDarFaraFamilyId_NuAdaugaFamilyIdInTokenClaims() {
        LoginRequest request = new LoginRequest();
        request.setEmail("parent@familie.com");
        request.setPassword("parola");

        User user = new User();
        user.setId(11L);
        user.setName("Parent User");
        user.setEmail("parent@familie.com");
        user.setPasswordH("parola");

        FamilyMember member = new FamilyMember();
        member.setRole("Parent");
        member.setFamily(null);

        when(userRepository.findByEmail("parent@familie.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parola", "parola")).thenReturn(true);
        when(familyMemberRepository.findByUserId(11L)).thenReturn(List.of(member));
        when(jwtUtil.generateToken(eq("parent@familie.com"), any())).thenReturn("parent-token");

        ResponseEntity<Object> result = authController.login(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("Parent", body.get("role"));

        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jwtUtil).generateToken(eq("parent@familie.com"), claimsCaptor.capture());
        Map<String, Object> claims = claimsCaptor.getValue();
        assertEquals(11L, claims.get("userId"));
        assertEquals("Parent", claims.get("role"));
        assertEquals("Parent User", claims.get("name"));
        assertFalse(claims.containsKey("familyId"));
    }

    @Test
    void login_CandUserInexistent_ReturneazaUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("none@example.com");
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        ResponseEntity<Object> result = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void login_CandParolaGresita_ReturneazaUnauthorized() {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setEmail("test@familie.com");
        request.setPassword("parolaGresita");

        User user = new User();
        user.setEmail("test@familie.com");
        user.setPasswordH("parolaBuna");

        when(userRepository.findByEmail("test@familie.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parolaGresita", "parolaBuna")).thenReturn(false);

        // WHEN
        ResponseEntity<Object> result = authController.login(request);

        // THEN
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void register_CandDateValide_CreeazaUserSiReturneazaToken() {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setName("Nou User");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setFavoriteAnimal("cat");
        request.setFavoriteColor("blue");
        request.setChildhoodStreet("Oak Street");

        Family savedFamily = new Family();
        savedFamily.setId(1L);

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
        when(familyRepository.save(any(Family.class))).thenReturn(savedFamily);
        when(jwtUtil.generateToken(eq("new@example.com"), any())).thenReturn("new-jwt-token");

        // WHEN
        ResponseEntity<Object> result = authController.register(request);

        // THEN
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        verify(userRepository, times(1)).save(any(User.class));
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertNotNull(body);
        assertEquals("new-jwt-token", body.get("token"));
    }

    @Test
    void register_CandEmailExistent_ReturneazaConflict() {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existent@example.com");
        request.setFavoriteAnimal("cat");
        request.setFavoriteColor("blue");
        request.setChildhoodStreet("Oak Street");

        when(userRepository.findByEmail("existent@example.com")).thenReturn(Optional.of(new User()));

        // WHEN
        ResponseEntity<Object> result = authController.register(request);

        // THEN
        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    void logout_CandTokenEsteValid_IlAdaugaInBlacklist() {
        // GIVEN
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.extractJti("valid-token")).thenReturn("jti-123");
        when(jwtUtil.extractExpiration("valid-token")).thenReturn(new Date(System.currentTimeMillis() + 10000));

        // WHEN
        ResponseEntity<Object> result = authController.logout(request);

        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(blacklistService, times(1)).revokeToken(eq("jti-123"), any(Date.class));
    }

    @Test
    void logout_CandTokenInvalidSauLipsaJti_ReturneazaBadRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
        when(jwtUtil.extractJti(any())).thenThrow(new RuntimeException("Fail"));

        ResponseEntity<Object> result = authController.logout(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void register_CandRolCopil_NuCreeazaFamilie() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Copil User");
        request.setEmail("copil@example.com");
        request.setPassword("pass123");
        request.setRole("Child");
        request.setFavoriteAnimal("dog");
        request.setFavoriteColor("green");
        request.setChildhoodStreet("Maple Ave");

        when(userRepository.findByEmail("copil@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("encoded-child-pass");
        when(jwtUtil.generateToken(eq("copil@example.com"), any())).thenReturn("child-token");

        ResponseEntity<Object> result = authController.register(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        verify(familyRepository, never()).save(any());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("Child", body.get("role"));
        assertEquals("child-token", body.get("token"));
    }

    @Test
    void forgotPassword_CandRaspunsurileSuntCorecte_ReturneazaVerificareReusita() {
        User user = new User();
        user.setId(3L);
        user.setEmail("reset@example.com");

        Answer answers = new Answer();
        answers.setAnimal("encoded-cat");
        answers.setColor("encoded-blue");
        answers.setStreet("encoded-street");

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("reset@example.com");
        request.setQuestion1(SecurityQuestion.ANIMAL);
        request.setAnswer1("cat");
        request.setQuestion2(SecurityQuestion.COLOR);
        request.setAnswer2("blue");

        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));
        when(answerRepository.findByUserId(3L)).thenReturn(Optional.of(answers));
        when(passwordEncoder.matches("cat", "encoded-cat")).thenReturn(true);
        when(passwordEncoder.matches("blue", "encoded-blue")).thenReturn(true);

        ResponseEntity<Object> result = authController.forgotPassword(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("Verificare reușită.", body.get("message"));
    }

    @Test
    void forgotPassword_CandIntrebarileSuntIdentice_ReturneazaBadRequest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("reset@example.com");
        request.setQuestion1(SecurityQuestion.ANIMAL);
        request.setAnswer1("cat");
        request.setQuestion2(SecurityQuestion.ANIMAL);
        request.setAnswer2("cat");

        ResponseEntity<Object> result = authController.forgotPassword(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void resetPassword_CandRaspunsurileSuntCorecte_ActualizeazaParola() {
        User user = new User();
        user.setId(4L);
        user.setEmail("reset2@example.com");

        Answer answers = new Answer();
        answers.setAnimal("encoded-dog");
        answers.setColor("encoded-green");
        answers.setStreet("encoded-road");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("reset2@example.com");
        request.setQuestion1(SecurityQuestion.ANIMAL);
        request.setAnswer1("dog");
        request.setQuestion2(SecurityQuestion.STREET);
        request.setAnswer2("road");
        request.setNewPassword("NewPassword123!");

        when(userRepository.findByEmail("reset2@example.com")).thenReturn(Optional.of(user));
        when(answerRepository.findByUserId(4L)).thenReturn(Optional.of(answers));
        when(passwordEncoder.matches("dog", "encoded-dog")).thenReturn(true);
        when(passwordEncoder.matches("road", "encoded-road")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("encoded-new-pass");

        ResponseEntity<Object> result = authController.resetPassword(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userRepository).save(argThat(savedUser -> "encoded-new-pass".equals(savedUser.getPasswordH())));
    }

    @Test
    void resetPassword_CandRaspunsurileSuntGresite_ReturneazaBadRequest() {
        User user = new User();
        user.setId(5L);
        user.setEmail("reset3@example.com");

        Answer answers = new Answer();
        answers.setAnimal("encoded-cat");
        answers.setColor("encoded-blue");
        answers.setStreet("encoded-street");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("reset3@example.com");
        request.setQuestion1(SecurityQuestion.ANIMAL);
        request.setAnswer1("wrong");
        request.setQuestion2(SecurityQuestion.COLOR);
        request.setAnswer2("blue");
        request.setNewPassword("NewPassword123!");

        when(userRepository.findByEmail("reset3@example.com")).thenReturn(Optional.of(user));
        when(answerRepository.findByUserId(5L)).thenReturn(Optional.of(answers));
        when(passwordEncoder.matches("wrong", "encoded-cat")).thenReturn(false);

        ResponseEntity<Object> result = authController.resetPassword(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_CandNuExistaRaspunsuri_ReturneazaBadRequest() {
        User user = new User();
        user.setId(6L);
        user.setEmail("missing-answers@example.com");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("missing-answers@example.com");
        request.setQuestion1(SecurityQuestion.ANIMAL);
        request.setAnswer1("cat");
        request.setQuestion2(SecurityQuestion.COLOR);
        request.setAnswer2("blue");
        request.setNewPassword("NewPassword123!");

        when(userRepository.findByEmail("missing-answers@example.com")).thenReturn(Optional.of(user));
        when(answerRepository.findByUserId(6L)).thenReturn(Optional.empty());

        ResponseEntity<Object> result = authController.resetPassword(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void forgotPassword_CandUtilizatorulNuExista_ReturneazaBadRequest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("unknown@example.com");
        request.setQuestion1(SecurityQuestion.ANIMAL);
        request.setAnswer1("cat");
        request.setQuestion2(SecurityQuestion.COLOR);
        request.setAnswer2("blue");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        ResponseEntity<Object> result = authController.forgotPassword(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void forgotPassword_CandUnRaspunsEsteNull_ReturneazaBadRequest() {
        User user = new User();
        user.setId(7L);
        user.setEmail("null-answer@example.com");

        Answer answers = new Answer();
        answers.setAnimal("encoded-cat");
        answers.setColor("encoded-blue");
        answers.setStreet("encoded-street");

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("null-answer@example.com");
        request.setQuestion1(SecurityQuestion.ANIMAL);
        request.setAnswer1(null);
        request.setQuestion2(SecurityQuestion.COLOR);
        request.setAnswer2("blue");

        when(userRepository.findByEmail("null-answer@example.com")).thenReturn(Optional.of(user));
        when(answerRepository.findByUserId(7L)).thenReturn(Optional.of(answers));
        when(passwordEncoder.matches("blue", "encoded-blue")).thenReturn(true);

        ResponseEntity<Object> result = authController.forgotPassword(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void refresh_CandUserAreFamilie_ReturneazaTokenNou() {
        User user = new User();
        user.setId(1L);
        user.setName("Alex");
        user.setEmail("alex@example.com");

        FamilyMember member = new FamilyMember();
        member.setRole("Parent");
        Family family = new Family();
        family.setId(10L);
        member.setFamily(family);

        org.springframework.security.core.Authentication auth =
                mock(org.springframework.security.core.Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(member));
        when(jwtUtil.generateToken(eq("alex@example.com"), any())).thenReturn("refreshed-token");

        ResponseEntity<Object> result = authController.refresh(auth);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("refreshed-token", body.get("token"));
        assertEquals("Parent", body.get("role"));
    }

    @Test
    void refresh_CandUserFaraFamilie_ReturneazaRoleParent() {
        User user = new User();
        user.setId(2L);
        user.setName("Nou");
        user.setEmail("nou@example.com");

        org.springframework.security.core.Authentication auth =
                mock(org.springframework.security.core.Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(2L)).thenReturn(Collections.emptyList());
        when(jwtUtil.generateToken(eq("nou@example.com"), any())).thenReturn("token-fara-familie");

        ResponseEntity<Object> result = authController.refresh(auth);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("Parent", body.get("role"));
    }

    @Test
    void refresh_CandRolEsteCopil_SiNuExistaFamilyId_NormalizeazaRolul() {
        User user = new User();
        user.setId(3L);
        user.setName("Copil Refresh");
        user.setEmail("copil-refresh@example.com");

        FamilyMember member = new FamilyMember();
        member.setRole("child");
        member.setFamily(null);

        org.springframework.security.core.Authentication auth =
                mock(org.springframework.security.core.Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(3L)).thenReturn(List.of(member));
        when(jwtUtil.generateToken(eq("copil-refresh@example.com"), any())).thenReturn("child-refresh-token");

        ResponseEntity<Object> result = authController.refresh(auth);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("Child", body.get("role"));

        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jwtUtil).generateToken(eq("copil-refresh@example.com"), claimsCaptor.capture());
        Map<String, Object> claims = claimsCaptor.getValue();
        assertEquals("Child", claims.get("role"));
        assertFalse(claims.containsKey("familyId"));
    }

    @Test
    void logout_CandHeaderNuEsteBearer_ReturneazaBadRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        ResponseEntity<Object> result = authController.logout(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verifyNoInteractions(blacklistService);
    }

    @Test
    void logout_CandJtiLipseste_ReturneazaBadRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token-fara-jti");
        when(jwtUtil.extractJti("token-fara-jti")).thenReturn(null);
        when(jwtUtil.extractExpiration("token-fara-jti")).thenReturn(new Date(System.currentTimeMillis() + 10000));

        ResponseEntity<Object> result = authController.logout(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verify(blacklistService, never()).revokeToken(any(), any());
    }

    @Test
    void logout_CandHeaderLipseste_ReturneazaBadRequest() {
        // GIVEN
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        // WHEN
        ResponseEntity<Object> result = authController.logout(request);

        // THEN
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }
}
