package com.example.integrated.oauth.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "oauth_users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 SNS 플랫폼인지 (ex: "google", "kakao")
    @Column(nullable = false)
    private String provider;

    // 해당 플랫폼의 고유 사용자 ID (Google의 경우 "sub" 값)
    @Column(nullable = false)
    private String providerId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    // 구글 프로필 사진 URL
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder
    public User(String provider, String providerId, String email,
                String name, String profileImageUrl, Role role) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
    }

    // 프로필 정보 갱신 (재로그인 시 최신 정보 업데이트)
    public User update(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        return this;
    }
}
