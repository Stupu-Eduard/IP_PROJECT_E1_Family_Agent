package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.AddMemberRequest;
import com.familie.cheltuieli_familie.dto.FamilyMemberDTO;
import com.familie.cheltuieli_familie.dto.InvitationDTO;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.service.FamilyService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FamilyControllerTest {

    @Mock private FamilyService familyService;
    @Mock private InvitationService invitationService;
    @InjectMocks private FamilyController controller;

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
    void createFamily_withName_returnsCreated() {
        Map<String, Object> result = Map.of("token", "tok", "role", "Parent", "familyId", 1L);
        when(familyService.createFamily("TestFamily", user)).thenReturn(result);

        ResponseEntity<Map<String, Object>> resp = controller.createFamily(Map.of("name", "TestFamily"), auth);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals("tok", resp.getBody().get("token"));
    }

    @Test
    void createFamily_nullBody_passesNullName() {
        Map<String, Object> result = Map.of("token", "tok", "role", "Parent", "familyId", 1L);
        when(familyService.createFamily(null, user)).thenReturn(result);

        ResponseEntity<Map<String, Object>> resp = controller.createFamily(null, auth);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        verify(familyService).createFamily(null, user);
    }

    @Test
    void getMembers_returnsOk() {
        FamilyMemberDTO dto = new FamilyMemberDTO(1L, 1L, "Alex", "alex@test.com", "Parent");
        when(familyService.getMembers(10L, user)).thenReturn(List.of(dto));

        ResponseEntity<List<FamilyMemberDTO>> resp = controller.getMembers(10L, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
    }

    @Test
    void inviteMember_returnsCreated() {
        AddMemberRequest req = new AddMemberRequest();
        req.setEmail("child@test.com");
        req.setRole("Child");
        InvitationDTO dto = new InvitationDTO(1L, 10L, "Family", "Alex", "Child");
        when(invitationService.createInvitation(10L, req, user)).thenReturn(dto);

        ResponseEntity<InvitationDTO> resp = controller.inviteMember(10L, req, auth);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals(dto, resp.getBody());
    }

    @Test
    void updateMemberRole_withBody_returnsOk() {
        FamilyMemberDTO dto = new FamilyMemberDTO(5L, 5L, "Bob", "bob@test.com", "Co-Parent");
        when(familyService.updateMemberRole(10L, 5L, "Co-Parent", user)).thenReturn(dto);

        ResponseEntity<FamilyMemberDTO> resp = controller.updateMemberRole(10L, 5L, Map.of("role", "Co-Parent"), auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Co-Parent", resp.getBody().getRole());
    }

    @Test
    void updateMemberRole_nullBody_passesNullRole() {
        FamilyMemberDTO dto = new FamilyMemberDTO(5L, 5L, "Bob", "bob@test.com", "Child");
        when(familyService.updateMemberRole(eq(10L), eq(5L), isNull(), eq(user))).thenReturn(dto);

        ResponseEntity<FamilyMemberDTO> resp = controller.updateMemberRole(10L, 5L, null, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(familyService).updateMemberRole(10L, 5L, null, user);
    }

    @Test
    void removeMember_returnsNoContent() {
        doNothing().when(familyService).removeMember(10L, 5L, user);

        ResponseEntity<Void> resp = controller.removeMember(10L, 5L, auth);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(familyService).removeMember(10L, 5L, user);
    }

    @Test
    void deleteFamily_returnsNoContent() {
        doNothing().when(familyService).deleteFamily(10L, user);

        ResponseEntity<Void> resp = controller.deleteFamily(10L, auth);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(familyService).deleteFamily(10L, user);
    }

    @Test
    void leaveFamily_returnsNoContent() {
        doNothing().when(familyService).leaveFamily(10L, user);

        ResponseEntity<Void> resp = controller.leaveFamily(10L, auth);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        verify(familyService).leaveFamily(10L, user);
    }
}
