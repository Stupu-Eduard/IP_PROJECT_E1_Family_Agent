package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    boolean existsByJti(String jti);
}
