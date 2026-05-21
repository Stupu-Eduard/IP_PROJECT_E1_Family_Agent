package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.AvatarResponseDTO;
import com.familie.cheltuieli_familie.model.Answer;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.AnswerRepository;
import com.familie.cheltuieli_familie.repository.BudgetRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.FamilyInvitationRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.FamilyRepository;
import com.familie.cheltuieli_familie.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private UserRepository             userRepository;
    @Mock private CloudinaryService          cloudinaryService;
    @Mock private FamilyMemberRepository     familyMemberRepository;
    @Mock private FamilyRepository           familyRepository;
    @Mock private ExpenseRepository          expenseRepository;
    @Mock private BudgetRepository           budgetRepository;
    @Mock private AnswerRepository           answerRepository;
    @Mock private FamilyInvitationRepository familyInvitationRepository;

    @InjectMocks
    private ProfileService profileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setName("Test User");
    }

    // ── updatePredefinedAvatar ────────────────────────────────────────────────

    @Test
    void updatePredefinedAvatar_savesAvatarIdAndReturnsDTO() {
        when(userRepository.save(user)).thenReturn(user);

        AvatarResponseDTO result = profileService.updatePredefinedAvatar("av_f_pink", user);

        assertEquals("av_f_pink", result.getAvatarUrl());
        assertEquals("av_f_pink", user.getAvatarUrl());
        verify(userRepository).save(user);
    }

    @Test
    void updatePredefinedAvatar_throwsBadRequest_whenAvatarIdIsNull() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> profileService.updatePredefinedAvatar(null, user));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePredefinedAvatar_throwsBadRequest_whenAvatarIdIsBlank() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> profileService.updatePredefinedAvatar("   ", user));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    // ── deleteAccount — user fără familie ────────────────────────────────────

    @Test
    void deleteAccount_userWithNoFamily_deletesUserAndCleanup() {
        when(familyMemberRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(answerRepository.findByUserId(1L)).thenReturn(Optional.empty());

        profileService.deleteAccount(user);

        verify(expenseRepository).nullifyUserFromExpenses(1L);
        verify(familyInvitationRepository).deleteByInviteeEmail("test@test.com");
        verify(familyInvitationRepository).deleteByInvitedById(1L);
        verify(userRepository).delete(user);
        verify(familyRepository, never()).delete(any());
    }

    // ── deleteAccount — singurul membru în familie ────────────────────────────

    @Test
    void deleteAccount_soleMemberInFamily_deletesEntireFamily() {
        Family family = new Family();
        family.setId(10L);

        FamilyMember membership = new FamilyMember();
        membership.setFamily(family);
        membership.setUser(user);

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(membership));
        when(familyMemberRepository.findByFamilyId(10L)).thenReturn(List.of(membership));
        when(answerRepository.findByUserId(1L)).thenReturn(Optional.empty());

        profileService.deleteAccount(user);

        verify(expenseRepository).clearFamilyFromExpenses(10L);
        verify(budgetRepository).clearFamilyFromBudgets(10L);
        verify(familyMemberRepository).deleteAll(List.of(membership));
        verify(familyRepository).delete(family);
        verify(expenseRepository).nullifyUserFromExpenses(1L);
        verify(userRepository).delete(user);
    }

    // ── deleteAccount — mai mulți membri în familie ───────────────────────────

    @Test
    void deleteAccount_multiMemberFamily_leavesWithoutDeletingFamily() {
        Family family = new Family();
        family.setId(10L);

        User otherUser = new User();
        otherUser.setId(2L);

        FamilyMember myMembership    = new FamilyMember();
        myMembership.setFamily(family);
        myMembership.setUser(user);

        FamilyMember otherMembership = new FamilyMember();
        otherMembership.setFamily(family);
        otherMembership.setUser(otherUser);

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(myMembership));
        when(familyMemberRepository.findByFamilyId(10L))
                .thenReturn(List.of(myMembership, otherMembership));
        when(answerRepository.findByUserId(1L)).thenReturn(Optional.empty());

        profileService.deleteAccount(user);

        // Familia NU e ștearsă
        verify(familyRepository, never()).delete(any());
        // Membrul e scos
        verify(familyMemberRepository).delete(myMembership);
        verify(userRepository).delete(user);
    }

    // ── deleteAccount — șterge răspunsurile de securitate ────────────────────

    @Test
    void deleteAccount_deletesSecurityAnswerWhenPresent() {
        Answer answer = new Answer();

        when(familyMemberRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(answerRepository.findByUserId(1L)).thenReturn(Optional.of(answer));

        profileService.deleteAccount(user);

        verify(answerRepository).delete(answer);
    }
}
