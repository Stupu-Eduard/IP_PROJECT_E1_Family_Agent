package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    // ID-ul fiind session_id, findById este suficient
}
