package com.example.integrated.oauth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // 로그인 성공! 프론트엔드의 /oauth/dashboard 페이지로 리다이렉트
        // 세션 쿠키(JSESSIONID)는 브라우저가 자동으로 보관합니다.
        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/oauth/dashboard");
    }
}
