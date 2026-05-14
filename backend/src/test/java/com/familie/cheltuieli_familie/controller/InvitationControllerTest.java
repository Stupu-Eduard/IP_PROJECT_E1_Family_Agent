package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.InvitationDTO;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.service.InvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvitationControllerTest {

    @Mock private InvitationService invitationService;
    @InjectMocks private InvitationController controller;

    private Authentication auth;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = mock(User.class);
        auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
    }

    @Test
    void getPending_returnsOkWithList() {
        InvitationDTO dto = new InvitationDTO(1L, 10L, "Family", "Alex", "Child");
        when(invitationService.getPendingForUser(user)).thenReturn(List.of(dto));

        ResponseEntity<List<InvitationDTO>> resp = controller.getPending(auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
        assertEquals(1L, resp.getBody().get(0).id());
    }

    @Test
    void getPending_emptyList_returnsOk() {
        when(invitationService.getPendingForUser(user)).thenReturn(List.of());

        ResponseEntity<List<InvitationDTO>> resp = controller.getPending(auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().isEmpty());
    }

    @Test
    void accept_returnsOkWithToken() {
        Map<String, Object> result = Map.of("token", "new-token", "role", "Child");
        when(invitationService.accept(1L, user)).thenReturn(result);

        ResponseEntity<Map<String, Object>> resp = controller.accept(1L, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("new-token", resp.getBody().get("token"));
    }

    @Test
    void decline_returnsNoContent() {
        doNothing().when(invitationService).decline(1L, user);

        ResponseEntity<Void> resp = controller.decline(1L, auth);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(invitationService).decline(1L, user);
    }
}
