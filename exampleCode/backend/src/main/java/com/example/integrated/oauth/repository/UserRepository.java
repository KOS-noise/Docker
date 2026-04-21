package com.example.integrated.oauth.repository;

import com.example.integrated.oauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // provider(플랫폼)와 providerId(플랫폼의 고유 ID) 조합으로 유저 찾기
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
