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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    private static final Set<String> ALLOWED_AVATAR_IDS = Set.of(
            "av_f_pink", "av_m_pink", "av_f_purple", "av_m_purple",
            "av_f_blue", "av_m_blue", "av_f_dark", "av_m_dark",
            "av_f_yellow", "av_m_yellow"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Transactional
    public AvatarResponseDTO updatePredefinedAvatar(String avatarId, User user) {
        if (avatarId == null || avatarId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "avatarId este obligatoriu.");
        }
        if (!ALLOWED_AVATAR_IDS.contains(avatarId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar ID invalid.");
        }
        user.setAvatarUrl(avatarId);
        userRepository.save(user);
        log.info("Avatar predefinit actualizat pentru userId={}", user.getId());
        return new AvatarResponseDTO(avatarId);
    }

    @Transactional
    public AvatarResponseDTO uploadAvatarFile(MultipartFile file, User user) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fișierul este gol.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format nepermis. Acceptă doar: JPEG, PNG, WEBP, GIF.");
        }

        // S2083 fix: prefix/suffix fix, fără date din user în numele fișierului temporar
        Path tempPath = null;
        try {
            tempPath = Files.createTempFile("avatar-upload-", "-" + UUID.randomUUID().toString() + ".tmp");
            file.transferTo(tempPath);

            String publicId = "user_" + user.getId() + "_avatar";
            String url = cloudinaryService.uploadFile(tempPath.toFile(), "avatars", publicId);

            user.setAvatarUrl(url);
            userRepository.save(user);
            log.info("Avatar uploadat pe Cloudinary pentru userId={}", user.getId());
            return new AvatarResponseDTO(url);

        } catch (IOException e) {
            log.error("Eroare la upload avatar pentru userId={}", user.getId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Eroare la procesarea fișierului.");
        } finally {
            if (tempPath != null) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException ex) {
                    log.warn("Nu s-a putut șterge fișierul temporar");
                }
            }
        }
    }

    @Transactional
    public void deleteAccount(User user) {
        Long userId = user.getId();
        log.info("Început ștergere cont pentru userId={}", userId);

        List<FamilyMember> memberships = familyMemberRepository.findByUserId(userId);
        for (FamilyMember membership : memberships) {
            Family family = membership.getFamily();
            if (family == null) continue;

            Long familyId = family.getId();
            List<FamilyMember> allMembers = familyMemberRepository.findByFamilyId(familyId);

            if (allMembers.size() == 1) {
                expenseRepository.clearFamilyFromExpenses(familyId);
                budgetRepository.clearFamilyFromBudgets(familyId);
                familyMemberRepository.deleteAll(allMembers);
                familyRepository.delete(family);
                log.info("Familie ștearsă (singur membru): familyId={}", familyId);
            } else {
                familyMemberRepository.delete(membership);
                log.info("User scos din familie: userId={}, familyId={}", userId, familyId);
            }
        }

        expenseRepository.nullifyUserFromExpenses(userId);
        answerRepository.findByUserId(userId).ifPresent(answerRepository::delete);
        familyInvitationRepository.deleteByInviteeEmail(user.getEmail());
        familyInvitationRepository.deleteByInvitedById(userId);

        userRepository.delete(user);
        log.info("Cont șters complet: userId={}", userId);
    }

    static String sanitizeForLog(String input) {
        if (input == null) return "null";
        return input.replace('\n', '_').replace('\r', '_');
    }
}