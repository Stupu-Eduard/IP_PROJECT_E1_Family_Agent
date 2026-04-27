package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User stefana, adela, alexandra;

    @BeforeEach
    void setUp() {
        String commonPass = "ingineriaprogramarii123";

        stefana = createUser("Stefana", "stefana.vultur@gmail.com", commonPass, LocalDate.now());
        adela = createUser("Adela", "adela.tirnovschi@gmail.com", commonPass, LocalDate.now().minusDays(2));
        alexandra = createUser("Alexandra", "alexandra.enachescu@gmail.com", commonPass, LocalDate.now().minusDays(5));

        entityManager.flush();
    }

    @Test
    @DisplayName("Gaseste utilizatorul dupa email-ul exact")
    void testFindUserByEmail() {
        Optional<User> foundUser = userRepository.findByEmail("stefana.vultur@gmail.com");

        assertTrue(foundUser.isPresent(), "Stefana ar trebui gasita in baza de date.");
        assertEquals("ingineriaprogramarii123", foundUser.get().getPasswordH(), "Parola nu se potriveste.");
    }

    @Test
    @DisplayName("Cauta utilizatori partial dupa nume ")
    void testFindByNameContaining() {
        List<User> searchResults = userRepository.findByNameContainingIgnoreCase("ade");

        boolean adelaFound = searchResults.stream().anyMatch(u -> u.getName().equals("Adela"));
        assertTrue(adelaFound, "Ar trebui gasit utilizatorul Adela pentru cautarea 'ade'");
    }

    @Test
    @DisplayName("Filtreaza utilizatorii inregistrati recent (dupa o anumita data)")
    void testFindByCreatedAtAfter() {
        LocalDate thresholdDate = LocalDate.now().minusDays(4);
        List<User> recentUsers = userRepository.findByCreatedAtAfter(thresholdDate);

        boolean stefanaFound = recentUsers.stream().anyMatch(u -> u.getEmail().equals("stefana.vultur@gmail.com"));
        boolean adelaFound = recentUsers.stream().anyMatch(u -> u.getEmail().equals("adela.tirnovschi@gmail.com"));
        boolean alexandraFound = recentUsers.stream().anyMatch(u -> u.getEmail().equals("alexandra.enachescu@gmail.com"));

        assertTrue(stefanaFound, "Stefana ar trebui sa apara in filtru (creata azi).");
        assertTrue(adelaFound, "Adela ar trebui sa apara in filtru (creata acum 2 zile).");
        assertFalse(alexandraFound, "Alexandra NU ar trebui sa apara in filtru (creata acum 5 zile).");
    }

    @Test
    @DisplayName("Simulare Login: Gaseste utilizatorul dupa Email si Parola")
    void testFindByEmailAndPassword() {
        Optional<User> loginSuccess = userRepository.findByEmailAndPasswordH(
                "stefana.vultur@gmail.com",
                "ingineriaprogramarii123"
        );

        assertTrue(loginSuccess.isPresent(), "Autentificarea trebuia sa reuseasca pentru date corecte.");
        assertEquals("Stefana", loginSuccess.get().getName());
    }

    @Test
    @DisplayName("Simulare Login Esuat: Parola gresita")
    void testFindByEmailAndPassword_WrongPassword() {
        Optional<User> loginFailed = userRepository.findByEmailAndPasswordH(
                "stefana.vultur@gmail.com",
                "o_parola_gresita"
        );

        assertFalse(loginFailed.isPresent(), "Nu ar trebui sa gaseasca utilizatorul daca parola e gresita.");
    }

    private User createUser(String name, String email, String pass, LocalDate date) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordH(pass);
        u.setCreatedAt(date);
        return entityManager.persist(u);
    }
}