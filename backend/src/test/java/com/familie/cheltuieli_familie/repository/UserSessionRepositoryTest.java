package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.model.UserSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserSessionRepositoryTest {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findById_ShouldReturnSavedSession() {
        // GIVEN
        User user = new User();
        user.setEmail("repo@test.com");
        user.setPasswordH("hash");
        user = userRepository.save(user);

        String sessionId = "repo-session-id";
        UserSession session = UserSession.builder()
                .id(sessionId)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        userSessionRepository.save(session);

        // WHEN
        Optional<UserSession> found = userSessionRepository.findById(sessionId);

        // THEN
        assertTrue(found.isPresent());
        assertEquals("repo@test.com", found.get().getUser().getEmail());
    }
}
