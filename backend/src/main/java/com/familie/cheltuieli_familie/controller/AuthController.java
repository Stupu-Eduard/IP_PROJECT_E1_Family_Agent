package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.LoginRequest;
import com.familie.cheltuieli_familie.dto.RegisterRequest;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "https://family-agent.me"} , allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        log.info("Încercare login pentru: {}", loginRequest.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        // Verificare utilizator și parola
        // NOTa: Momentan verificăm parola în mod simplu (fără criptare complexă, folosim doar un match pe câmpul passwordH)
        if (userOpt.isPresent() && userOpt.get().getPasswordH().equals(loginRequest.getPassword())) {
            User user = userOpt.get();

            // 1. Generam un Session ID unic
            String sessionId = UUID.randomUUID().toString();

            // 2. Salvam sesiunea în baza de date
            UserSession session = UserSession.builder()
                    .sessionToken(sessionId)
                    .user(user)
                    .lastActive(LocalDateTime.now())
                    .build();
            sessionRepository.save(session);

            // 3. Cream Cookie-ul și îl trimitem către browser
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

    @PostMapping("/register")
    public ResponseEntity<Object> register(@Valid @RequestBody RegisterRequest registerRequest, HttpServletResponse response) {
        log.info("Încercare înregistrare pentru: {}", registerRequest.getEmail());

        // Verificare dacă emailul există deja
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email deja asociat unui cont."));
        }

        // Creare utilizator nou
        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPasswordH(registerRequest.getPassword());
        user.setCreatedAt(LocalDate.now());
        userRepository.save(user);

        // Creare sesiune (similar cu login)
        String sessionId = UUID.randomUUID().toString();
        UserSession session = UserSession.builder()
                .sessionToken(sessionId)
                .user(user)
                .lastActive(LocalDateTime.now())
                .build();
        sessionRepository.save(session);

        // Creare Cookie
        String cookieHeader = String.format(
                "session_id=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Lax",
                sessionId, 24 * 60 * 60
        );
        response.addHeader("Set-Cookie", cookieHeader);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Înregistrare realizată cu succes!",
                        "userName", user.getName(),
                        "token", sessionId
                ));
    }
}
