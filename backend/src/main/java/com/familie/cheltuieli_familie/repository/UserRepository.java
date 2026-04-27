package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    List<User> findByNameContainingIgnoreCase(String nameFragment);
    List<User> findByCreatedAtAfter(LocalDate date);
    Optional<User> findByEmailAndPasswordH(String email, String passwordH);
}