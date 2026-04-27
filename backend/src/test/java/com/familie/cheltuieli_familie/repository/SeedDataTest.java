package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@ActiveProfiles("test")
public class SeedDataTest {

    @Autowired
    private UserRepository userRepository;
    @Test
    @Sql(statements = "INSERT INTO users (id, name, email, password_h) VALUES (99, 'User Test', 'test@seed.com', 'pass')")
    void checkSeedUsers() {
        boolean empty = userRepository.findAll().isEmpty();
        assertFalse(empty, "Nu au fost găsite date de tip seed!");
    }
}