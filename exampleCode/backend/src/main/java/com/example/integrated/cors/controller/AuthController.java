package com.example.integrated.cors.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cors")
public class AuthController {

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginData, HttpServletResponse response) {
        String username = loginData.get("username");

        // 실무라면 DB를 검증하지만, 실습이므로 무조건 통과!
        String fakeAccessToken = "ey_" + username + "_access_token_12345";
        String fakeRefreshToken = UUID.randomUUID().toString();

        // [핵심] HttpOnly 쿠키 발급 (XSS 방어용)
        Cookie refreshCookie = new Cookie("refreshToken", fakeRefreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");

        // 원래는 SameSite, Secure 설정도 해야하지만 실습 환경(http)을 고려해 생략
        response.addCookie(refreshCookie);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", fakeAccessToken);
        return result;
    }

    // 2. 보안 인가 API (요청에 Cookie와 Token이 둘 다 있어야 통과되는 척)
    @GetMapping("/secure/me")
    public String getMyInfo(
            // 쿠키에 실린 값을 봅니다
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            // 헤더에 실린 토큰을 봅니다
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "❌ [오류] 헤더에 인증 토큰이 없습니다. 전역변수를 확인하세요.";
        }

        if (refreshToken == null) {
            return "❌ [오류] 쿠키(refreshToken)가 전달되지 않았습니다. 프론트엔드의 credentials 옵션이나 백엔드 allowCredentials를 확인하세요!";
        }

        return "✅ [성공] 쿠키(" + refreshToken.substring(0, 8) + "...)와 토큰(" + authHeader + ")을 모두 확인했습니다. 안전하게 응답합니다.";
    }
}
