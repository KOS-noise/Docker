package com.example.integrated.oauth.controller;

import com.example.integrated.oauth.entity.User;
import com.example.integrated.oauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // 현재 로그인된 유저 정보 반환 (프론트에서 세션 확인용)
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyInfo(
            @AuthenticationPrincipal OAuth2User oAuth2User) {

        if (oAuth2User == null) {
            return ResponseEntity.status(401).build();
        }

        // 구글에서 받아온 attributes 중 필요한 것만 골라서 반환
        Map<String, Object> result = new HashMap<>();
        result.put("name",    oAuth2User.getAttribute("name"));
        result.put("email",   oAuth2User.getAttribute("email"));
        result.put("picture", oAuth2User.getAttribute("picture"));
        result.put("sub",     oAuth2User.getAttribute("sub"));

        return ResponseEntity.ok(result);
    }

    // 관리자용: DB에 가입된 전체 유저 목록 조회
    @GetMapping("/oauth/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // 공개 API: 백엔드 동작 확인용
    @GetMapping("/public/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("서버가 정상 동작 중입니다.");
    }
}
