package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.LoginRequest;
import com.familie.cheltuieli_familie.dto.RegisterRequest;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.FamilyRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import com.familie.cheltuieli_familie.security.service.TokenBlacklistService;
import com.familie.cheltuieli_familie.security.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String ROLE_PARENT  = "Parent";
    private static final String ROLE_CHILD   = "Child";
    private static final String MSG_KEY      = "message";
    private static final String ERR_KEY      = "error";
    private static final String TOKEN_KEY    = "token";
    private static final String USER_ID_KEY  = "userId";
    private static final String FAMILY_ID_KEY = "familyId";

    private final UserRepository userRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyRepository familyRepository;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Încercare login pentru: {}", loginRequest.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        if (userOpt.isPresent() && passwordEncoder.matches(loginRequest.getPassword(), userOpt.get().getPasswordH())) {
            User user = userOpt.get();

            List<FamilyMember> memberships = familyMemberRepository.findByUserId(user.getId());
            String role = memberships.isEmpty() ? ROLE_PARENT : memberships.get(0).getRole();

            // Normalizăm rolul (frontend se așteaptă la "Parent" sau "Child")
            if (role.equalsIgnoreCase("parent")) role = ROLE_PARENT;
            else if (role.equalsIgnoreCase("child")) role = ROLE_CHILD;

            Map<String, Object> claims = new HashMap<>();
            claims.put(USER_ID_KEY, user.getId());
            claims.put("role", role);
            claims.put("name", user.getName());
            
            if (!memberships.isEmpty() && memberships.get(0).getFamily() != null) {
                claims.put(FAMILY_ID_KEY,memberships.get(0).getFamily().getId());
            }

            String token = jwtUtil.generateToken(user.getEmail(), claims);

            return ResponseEntity.ok(Map.of(
                    MSG_KEY, "Login realizat cu succes!",
                    TOKEN_KEY, token,
                    "userName", user.getName(),
                    "role", role
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(ERR_KEY, "Email sau parolă incorectă."));
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Încercare înregistrare pentru: {}", registerRequest.getEmail());

        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(ERR_KEY, "Email deja asociat unui cont."));
        }

        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPasswordH(passwordEncoder.encode(registerRequest.getPassword()));
        user.setCreatedAt(java.time.LocalDate.now());
        userRepository.save(user);

        boolean isChild = ROLE_CHILD.equalsIgnoreCase(registerRequest.getRole());

        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_KEY, user.getId());
        claims.put("name", user.getName());

        String role;
        if (isChild) {
            role = ROLE_CHILD;
            claims.put("role", ROLE_CHILD);
            log.info("Cont de copil creat fără familie: {}", user.getEmail());
        } else {
            role = ROLE_PARENT;
            claims.put("role", ROLE_PARENT);

            Family family = new Family();
            family.setName(registerRequest.getName() + "'s Family");
            family.setCreatedAt(java.time.LocalDate.now());
            Family savedFamily = familyRepository.save(family);

            FamilyMember member = new FamilyMember();
            member.setUser(user);
            member.setFamily(savedFamily);
            member.setRole(ROLE_PARENT);
            familyMemberRepository.save(member);

            claims.put(FAMILY_ID_KEY,savedFamily.getId());
            log.info("Familie creată automat pentru noul părinte: {} (familyId={})", user.getEmail(), savedFamily.getId());
        }

        String token = jwtUtil.generateToken(user.getEmail(), claims);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        MSG_KEY, "Înregistrare realizată cu succes!",
                        TOKEN_KEY, token,
                        "userName", user.getName(),
                        "role", role
                ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Object> refresh(Authentication auth) {
        User user = (User) auth.getPrincipal();
        List<FamilyMember> memberships = familyMemberRepository.findByUserId(user.getId());

        String role = memberships.isEmpty() ? ROLE_PARENT : memberships.get(0).getRole();
        if (role.equalsIgnoreCase("parent")) role = ROLE_PARENT;
        else if (role.equalsIgnoreCase("child")) role = ROLE_CHILD;

        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_KEY, user.getId());
        claims.put("role", role);
        claims.put("name", user.getName());
        if (!memberships.isEmpty() && memberships.get(0).getFamily() != null) {
            claims.put(FAMILY_ID_KEY,memberships.get(0).getFamily().getId());
        }

        String token = jwtUtil.generateToken(user.getEmail(), claims);
        log.info("Token reîmprospătat pentru: {}", user.getEmail());
        return ResponseEntity.ok(Map.of(TOKEN_KEY, token, "role", role));
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String jti = jwtUtil.extractJti(token);
                Date expiration = jwtUtil.extractExpiration(token);
                
                if (jti != null && expiration != null) {
                    blacklistService.revokeToken(jti, expiration);
                    log.info("✅ Sesiune delogată și token revocat cu succes.");
                    return ResponseEntity.ok(Map.of(MSG_KEY, "Delogare realizată cu succes."));
                }
            } catch (Exception e) {
                log.error("Eroare la delogare: {}", e.getMessage());
            }
        }
        return ResponseEntity.badRequest().body(Map.of(ERR_KEY, "Token invalid sau inexistent."));
    }
}
