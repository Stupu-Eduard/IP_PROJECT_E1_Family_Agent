package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.LoginRequest;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.model.UserSession;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.repository.UserSessionRepository;
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
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        log.info("Încercare login pentru: {}", loginRequest.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        // Verificare utilizator și parolă
        if (userOpt.isPresent() && userOpt.get().getPasswordH().equals(loginRequest.getPassword())) {
            User user = userOpt.get();

            // 1. Generăm un Session ID unic (pentru Cookie)
            String sessionId = UUID.randomUUID().toString();

            // 2. GENERĂM TOKEN-UL ANTI-CSRF (Task 1 - Parola secretă)
            String csrfToken = UUID.randomUUID().toString();

            // 3. Salvăm ambele token-uri în baza de date
            UserSession session = UserSession.builder()
                    .sessionToken(sessionId)
                    .csrfToken(csrfToken) // Salvăm token-ul în noua coloană creată de colegul tău
                    .user(user)
                    .lastActive(LocalDateTime.now())
                    .build();
            sessionRepository.save(session);

            // 4. Creăm Cookie-ul și îl trimitem către browser
            String cookieHeader = String.format(
                    "session_id=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Lax",
                    sessionId, 24 * 60 * 60
            );
            response.addHeader("Set-Cookie", cookieHeader);

            // IMPORTANT: Trimitem csrfToken în corpul răspunsului JSON
            // pentru ca aplicația de React să îl poată memora și folosi la următoarele request-uri
            return ResponseEntity.ok(Map.of(
                    "message", "Login realizat cu succes!",
                    "userName", user.getName(),
                    "csrfToken", csrfToken
            ));
        }

        // Returnăm 401 Unauthorized dacă emailul sau parola nu sunt bune
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Email sau parolă incorectă."));
    }
}