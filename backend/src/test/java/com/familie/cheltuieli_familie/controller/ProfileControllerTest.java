package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.AvatarResponseDTO;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.security.util.SecurityService;
import com.familie.cheltuieli_familie.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileControllerTest {

    @Mock private ProfileService  profileService;
    @Mock private SecurityService securityService;
    @InjectMocks private ProfileController controller;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setName("Test User");
    }

    // ── updatePredefinedAvatar ────────────────────────────────────────────────

    @Test
    void updatePredefinedAvatar_returnsOkWithAvatarUrl() {
        when(securityService.getCurrentUser()).thenReturn(user);
        AvatarResponseDTO dto = new AvatarResponseDTO("av_f_pink");
        when(profileService.updatePredefinedAvatar("av_f_pink", user)).thenReturn(dto);

        ResponseEntity<AvatarResponseDTO> resp =
                controller.updatePredefinedAvatar(Map.of("avatarId", "av_f_pink"));

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("av_f_pink", resp.getBody().getAvatarUrl());
        verify(profileService).updatePredefinedAvatar("av_f_pink", user);
    }

    @Test
    void updatePredefinedAvatar_throwsUnauthorized_whenUserIsNull() {
        when(securityService.getCurrentUser()).thenReturn(null);

        // Extragem Map-ul în afara expresiei lambda pentru a satisface SonarQube
        Map<String, String> requestBody = Map.of("avatarId", "av_f_pink");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.updatePredefinedAvatar(requestBody));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(profileService, never()).updatePredefinedAvatar(any(), any());
    }

    // ── uploadAvatarFile ─────────────────────────────────────────────────────

    @Test
    void uploadAvatarFile_returnsOkWithCloudinaryUrl() {
        when(securityService.getCurrentUser()).thenReturn(user);
        MultipartFile file = mock(MultipartFile.class);
        AvatarResponseDTO dto = new AvatarResponseDTO("https://res.cloudinary.com/demo/avatar.jpg");
        when(profileService.uploadAvatarFile(file, user)).thenReturn(dto);

        ResponseEntity<AvatarResponseDTO> resp = controller.uploadAvatarFile(file);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("https://res.cloudinary.com/demo/avatar.jpg", resp.getBody().getAvatarUrl());
        verify(profileService).uploadAvatarFile(file, user);
    }

    @Test
    void uploadAvatarFile_throwsUnauthorized_whenUserIsNull() {
        when(securityService.getCurrentUser()).thenReturn(null);
        MultipartFile file = mock(MultipartFile.class);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.uploadAvatarFile(file));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(profileService, never()).uploadAvatarFile(any(), any());
    }

    // ── deleteAccount ────────────────────────────────────────────────────────

    @Test
    void deleteAccount_returnsNoContent() {
        when(securityService.getCurrentUser()).thenReturn(user);
        doNothing().when(profileService).deleteAccount(user);

        ResponseEntity<Void> resp = controller.deleteAccount();

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(profileService).deleteAccount(user);
    }

    @Test
    void deleteAccount_throwsUnauthorized_whenUserIsNull() {
        when(securityService.getCurrentUser()).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.deleteAccount());

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(profileService, never()).deleteAccount(any());
    }
}