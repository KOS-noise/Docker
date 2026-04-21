package com.example.integrated.config;

import com.example.integrated.oauth.handler.OAuth2SuccessHandler;
import com.example.integrated.oauth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // 1. CORS 설정이 필요 (WebConfig 설정과 별도로 보안 필터 수준에서 처리)

                                // 2. CSRF 비활성화 (REST API 방식 실습이므로)
                                .csrf(AbstractHttpConfigurer::disable)

                                // 3. 요청별 인가 설정
                                .authorizeHttpRequests(auth -> auth
                                                // [CORS 실습] 인증 없이 접근 가능
                                                .requestMatchers("/api/cors/**").permitAll()
                                                // [Valid 실습] 인증 없이 접근 가능
                                                .requestMatchers("/api/valid/**").permitAll()
                                                // [OAuth 실습] 공개 허용 경로
                                                .requestMatchers(
                                                                "/",
                                                                "/api/public/**",
                                                                "/h2-console/**",
                                                                "/oauth2/**",
                                                                "/login/**",
                                                                "/error")
                                                .permitAll()
                                                // 나머지 모든 요청은 로그인 필요
                                                .anyRequest().authenticated())

                                // 4. H2 콘솔을 iframe으로 열기 위한 설정
                                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                                // 5. OAuth2 로그인 설정 (핵심!)
                                .oauth2Login(oauth2 -> oauth2
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(oAuth2SuccessHandler))

                                // 6. 로그아웃 설정
                                .logout(logout -> logout
                                                .logoutUrl("/api/logout")
                                                .logoutSuccessUrl("http://localhost:5173")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID"));

                return http.build();
        }

}
