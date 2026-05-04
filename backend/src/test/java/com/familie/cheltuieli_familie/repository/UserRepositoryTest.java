package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;


    @Test
    @DisplayName("Create user - should persist and generate ID")
    void shouldSaveUserAndGenerateId() {
        User user = new User();
        user.setName("Alex");
        user.setEmail("alex@test.com");
        user.setPasswordH("pass123");
        user.setCreatedAt(LocalDate.now());

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
    }


    @Test
    @DisplayName("Read user - should retrieve by ID")
    void shouldFindUserById() {
        User user = new User();
        user.setName("Maria");
        user.setEmail("maria@test.com");
        user.setPasswordH("pass123");
        user.setCreatedAt(LocalDate.now());

        User saved = userRepository.save(user);

        User found = userRepository.findById(saved.getId()).orElse(null);

        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals("Maria", found.getName());
        assertEquals("maria@test.com", found.getEmail());
    }


    @Test
    @DisplayName("Update user - should persist changes")
    void shouldUpdateUser() {
        User user = new User();
        user.setName("OldName");
        user.setEmail("update@test.com");
        user.setPasswordH("pass");
        user.setCreatedAt(LocalDate.now());

        User saved = userRepository.save(user);

        saved.setName("NewName");
        User updated = userRepository.save(saved);

        assertEquals("NewName", updated.getName());
    }


    @Test
    @DisplayName("Delete user - should remove entity")
    void shouldDeleteUser() {
        User user = new User();
        user.setName("ToDelete");
        user.setEmail("delete@test.com");
        user.setPasswordH("pass");
        user.setCreatedAt(LocalDate.now());

        User saved = userRepository.save(user);

        Long id = saved.getId();

        userRepository.deleteById(id);

        assertFalse(userRepository.findById(id).isPresent());
    }


    @Test
    @DisplayName("Should enforce UNIQUE constraint on email")
    void shouldEnforceUniqueEmail() {
        User u1 = new User();
        u1.setName("User1");
        u1.setEmail("unique@test.com");
        u1.setPasswordH("pass1");
        u1.setCreatedAt(LocalDate.now());

        User u2 = new User();
        u2.setName("User2");
        u2.setEmail("unique@test.com");
        u2.setPasswordH("pass2");
        u2.setCreatedAt(LocalDate.now());

        userRepository.save(u1);

        assertThrows(Exception.class, () -> {
            userRepository.saveAndFlush(u2);
        });
    }


    @Test
    @DisplayName("Should fail when email is null")
    void shouldFailWhenEmailIsNull() {
        User user = new User();
        user.setName("Invalid");
        user.setPasswordH("pass");
        user.setCreatedAt(LocalDate.now());

        assertThrows(Exception.class, () -> {
            userRepository.saveAndFlush(user);
        });
    }

    @Test
    @DisplayName("Should fail when password is null")
    void shouldFailWhenPasswordIsNull() {
        User user = new User();
        user.setName("Invalid");
        user.setEmail("invalid@test.com");
        user.setCreatedAt(LocalDate.now());

        assertThrows(Exception.class, () -> {
            userRepository.saveAndFlush(user);
        });
    }


    @Test
    @DisplayName("Should return true when user exists by ID")
    void shouldCheckExistsById() {
        User user = new User();
        user.setName("Exists");
        user.setEmail("exists@test.com");
        user.setPasswordH("pass");

        User saved = userRepository.save(user);

        assertTrue(userRepository.existsById(saved.getId()));
    }
}