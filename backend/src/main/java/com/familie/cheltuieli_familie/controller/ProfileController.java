package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.AvatarResponseDTO;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.security.util.SecurityService;
import com.familie.cheltuieli_familie.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService  profileService;
    private final SecurityService securityService;

    // ── PATCH /api/v1/profile/avatar ──────────────────────────────────────────
    // Salvează un avatar predefinit (ex: "av_f_pink") în DB

    @PatchMapping("/avatar")
    public ResponseEntity<AvatarResponseDTO> updatePredefinedAvatar(
            @RequestBody Map<String, String> body) {

        User user = requireCurrentUser();
        String avatarId = body.get("avatarId");
        AvatarResponseDTO response = profileService.updatePredefinedAvatar(avatarId, user);
        return ResponseEntity.ok(response);
    }

    // ── POST /api/v1/profile/avatar/upload ────────────────────────────────────
    // Uploadează o poză pe Cloudinary și salvează URL-ul în DB

    @PostMapping("/avatar/upload")
    public ResponseEntity<AvatarResponseDTO> uploadAvatarFile(
            @RequestParam("file") MultipartFile file) {

        User user = requireCurrentUser();
        AvatarResponseDTO response = profileService.uploadAvatarFile(file, user);
        return ResponseEntity.ok(response);
    }

    // ── DELETE /api/v1/profile/account ────────────────────────────────────────
    // Șterge contul curent și toate datele asociate

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount() {
        User user = requireCurrentUser();
        profileService.deleteAccount(user);
        return ResponseEntity.noContent().build();
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private User requireCurrentUser() {
        User user = securityService.getCurrentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilizator neautentificat.");
        }
        return user;
    }
}
