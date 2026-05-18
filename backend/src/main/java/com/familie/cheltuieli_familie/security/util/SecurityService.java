package com.familie.cheltuieli_familie.security.util;

import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityService {

    private final FamilyMemberRepository familyMemberRepository;

    public Long[] resolveScope() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return new Long[]{null, null};
        }
        Long userId = user.getId();
        Long familyId = familyMemberRepository.findByUserId(userId).stream()
                .findFirst()
                .map(fm -> fm.getFamily() != null ? fm.getFamily().getId() : null)
                .orElse(null);
        return new Long[]{familyId, userId};
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
