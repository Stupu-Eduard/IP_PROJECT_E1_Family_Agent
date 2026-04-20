package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Test
    void list_filtersOutNullAndBlankNames() {
        UserRepository userRepository = mock(UserRepository.class);
        UserController controller = new UserController(userRepository);

        User u1 = new User();
        u1.setName("Ana");

        User u2 = new User();
        u2.setName("");

        User u3 = new User();
        u3.setName(null);

        when(userRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
                .thenReturn(List.of(u1, u2, u3));

        List<String> result = controller.list();

        assertEquals(List.of("Ana"), result);
        verify(userRepository).findAll(Sort.by(Sort.Direction.ASC, "name"));
    }
}
