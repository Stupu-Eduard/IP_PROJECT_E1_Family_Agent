package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.FamilyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyInvitationRepository extends JpaRepository<FamilyInvitation, Long> {

    List<FamilyInvitation> findByInviteeEmailAndStatus(String email, String status);

    Optional<FamilyInvitation> findByFamilyIdAndInviteeEmail(Long familyId, String email);

    boolean existsByFamilyIdAndInviteeEmailAndStatus(Long familyId, String email, String status);

    /** Șterge invitațiile trimise de un user (invited_by). Apelat la ștergerea contului. */
    void deleteByInvitedById(Long invitedById);
}