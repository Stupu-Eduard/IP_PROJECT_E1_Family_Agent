package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.AvatarResponseDTO;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository             userRepository;
    private final CloudinaryService          cloudinaryService;
    private final FamilyMemberRepository     familyMemberRepository;
    private final FamilyRepository           familyRepository;
    private final ExpenseRepository          expenseRepository;
    private final BudgetRepository           budgetRepository;
    private final AnswerRepository           answerRepository;
    private final FamilyInvitationRepository familyInvitationRepository;

    // ── Avatar predefinit ─────────────────────────────────────────────────────

    @Transactional
    public AvatarResponseDTO updatePredefinedAvatar(String avatarId, User user) {
        if (avatarId == null || avatarId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "avatarId este obligatoriu.");
        }
        user.setAvatarUrl(avatarId);
        userRepository.save(user);
        log.info("Avatar predefinit actualizat pentru {}: {}", user.getEmail(), avatarId);
        return new AvatarResponseDTO(avatarId);
    }

    // ── Upload poză Cloudinary ────────────────────────────────────────────────

    @Transactional
    public AvatarResponseDTO uploadAvatarFile(MultipartFile file, User user) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fișierul este gol.");
        }

        File tempFile = null;
        try {
            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "avatar";
            String ext = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.')) : ".jpg";

            tempFile = File.createTempFile("avatar_" + user.getId() + "_", ext);
            file.transferTo(tempFile);

            String url = cloudinaryService.uploadFile(tempFile, "avatars", "user_" + user.getId() + "_avatar");

            user.setAvatarUrl(url);
            userRepository.save(user);
            log.info("Avatar uploadat pe Cloudinary pentru {}: {}", user.getEmail(), url);
            return new AvatarResponseDTO(url);

        } catch (IOException e) {
            log.error("Eroare la upload avatar pentru {}", user.getEmail(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Eroare la procesarea fișierului.");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    // ── Ștergere cont ─────────────────────────────────────────────────────────

    @Transactional
    public void deleteAccount(User user) {
        Long userId = user.getId();
        log.info("Început ștergere cont pentru userId={}, email={}", userId, user.getEmail());

        // 1. Gestionăm familiile
        List<FamilyMember> memberships = familyMemberRepository.findByUserId(userId);
        for (FamilyMember membership : memberships) {
            Family family = membership.getFamily();
            if (family == null) continue;

            Long familyId = family.getId();
            List<FamilyMember> allMembers = familyMemberRepository.findByFamilyId(familyId);

            if (allMembers.size() == 1) {
                // Singurul membru — ștergem toată familia
                expenseRepository.clearFamilyFromExpenses(familyId);
                budgetRepository.clearFamilyFromBudgets(familyId);
                familyMemberRepository.deleteAll(allMembers);
                familyRepository.delete(family);
                log.info("Familie ștearsă (singur membru): familyId={}", familyId);
            } else {
                // Există și alți membri — ieșim doar noi
                familyMemberRepository.delete(membership);
                log.info("User scos din familie: userId={}, familyId={}", userId, familyId);
            }
        }

        // 2. Decuplăm cheltuielile userului (rămân în familie cu user_id = NULL)
        expenseRepository.nullifyUserFromExpenses(userId);

        // 3. Ștergem răspunsurile la întrebările de securitate
        answerRepository.findByUserId(userId).ifPresent(answerRepository::delete);

        // 4. Ștergem invitațiile primite (după email) și trimise (după invitedBy)
        familyInvitationRepository.deleteByInviteeEmail(user.getEmail());
        familyInvitationRepository.deleteByInvitedById(userId);

        // 5. Ștergem userul
        userRepository.delete(user);
        log.info("Cont șters complet: userId={}", userId);
    }
}
