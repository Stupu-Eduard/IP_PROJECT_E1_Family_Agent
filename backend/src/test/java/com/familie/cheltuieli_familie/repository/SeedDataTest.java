package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SeedDataTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Seed data check – should verify that SQL seed data is loaded correctly")
    @Sql(statements = "INSERT INTO users (id, name, email, password_h) VALUES (99, 'User Seed Test', 'seed@test.com', 'pass')")
    void shouldVerifySeedDataIsLoaded() {
        // Verificăm dacă datele inserate prin @Sql sunt accesibile prin repository
        Optional<User> seedUser = userRepository.findById(99L);

        assertTrue(seedUser.isPresent(), "Eroare: Datele de tip seed (ID 99) nu au fost găsite în baza de date!");
        assertEquals("User Seed Test", seedUser.get().getName());
        assertEquals("seed@test.com", seedUser.get().getEmail());
    }
}