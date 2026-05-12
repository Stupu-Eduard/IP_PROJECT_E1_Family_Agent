package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findByUserId(Long userId);
}