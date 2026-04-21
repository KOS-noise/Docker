package com.example.integrated.valid.repository;

import com.example.integrated.valid.entity.ValidUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidUserRepository extends JpaRepository<ValidUser, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
