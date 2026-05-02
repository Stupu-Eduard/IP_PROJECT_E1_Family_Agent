package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.LoginRequest;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.model.UserSession;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.repository.UserSessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        log.info("Încercare login pentru: {}", loginRequest.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        // Verificare utilizator și parolă
        // NOTĂ: Momentan verificăm parola în mod simplu (fără criptare complexă, folosim doar un match pe câmpul passwordH)
        // Dacă proiectul are un sistem de hash (ex. BCrypt), ar trebui folosit BCryptPasswordEncoder.matches() aici.
        if (userOpt.isPresent() && userOpt.get().getPasswordH().equals(loginRequest.getPassword())) {
            User user = userOpt.get();

            // 1. Generăm un Session ID unic
            String sessionId = UUID.randomUUID().toString();

            // 2. Salvăm sesiunea în baza de date (valabilă 24 de ore)
            UserSession session = UserSession.builder()
                    .id(sessionId)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();
            sessionRepository.save(session);

            // 3. Creăm Cookie-ul și îl trimitem către browser
            // Folosim un String pentru header pentru a avea control total asupra proprietăților (SameSite)
            String cookieHeader = String.format(
                    "session_id=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Lax",
                    sessionId, 24 * 60 * 60
            );
            response.addHeader("Set-Cookie", cookieHeader);

            return ResponseEntity.ok(Map.of(
                    "message", "Login realizat cu succes!",
                    "userName", user.getName()
            ));
        }

        // Returnăm 401 Unauthorized dacă emailul sau parola nu sunt bune
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Email sau parolă incorectă."));
    }
}
