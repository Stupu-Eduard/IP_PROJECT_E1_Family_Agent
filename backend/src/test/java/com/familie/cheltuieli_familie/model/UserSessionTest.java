package com.familie.cheltuieli_familie.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class UserSessionTest {

    @Test
    void isValid_ShouldReturnTrue_WhenLastActiveIsRecent() {
        UserSession session = new UserSession();
        session.setLastActive(LocalDateTime.now().minusHours(5));
        assertTrue(session.isValid());
    }

    @Test
    void isValid_ShouldReturnFalse_WhenLastActiveIsOld() {
        UserSession session = new UserSession();
        session.setLastActive(LocalDateTime.now().minusDays(2));
        assertFalse(session.isValid());
    }

    @Test
    void isValid_ShouldReturnFalse_WhenLastActiveIsNull() {
        UserSession session = new UserSession();
        session.setLastActive(null);
        assertFalse(session.isValid());
    }
}
