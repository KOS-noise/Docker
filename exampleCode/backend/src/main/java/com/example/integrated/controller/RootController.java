package com.example.integrated.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RootController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/")
    public String index() {
        try {
            // DB 연결 확인을 위한 간단한 쿼리 실행
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "<html>" +
                   "<head><meta charset='UTF-8'><title>Backend Status</title></head>" +
                   "<body>" +
                   "<h1>🚀 Backend is Running!</h1>" +
                   "<p style='color: green; font-size: 1.2em; font-weight: bold;'>✅ Database Connection: OK</p>" +
                   "<hr>" +
                   "<h3>Available Endpoints:</h3>" +
                   "<ul>" +
                   "<li><a href='/api/cors/test'>CORS Test (API)</a></li>" +
                   "<li><a href='/api/valid/test'>Validation Test (API)</a></li>" +
                   "<li><a href='/oauth2/authorization/google'>Google Login (OAuth2)</a></li>" +
                   "</ul>" +
                   "</body>" +
                   "</html>";
        } catch (Exception e) {
            return "<html>" +
                   "<head><meta charset='UTF-8'><title>Backend Status</title></head>" +
                   "<body>" +
                   "<h1>🚀 Backend is Running!</h1>" +
                   "<p style='color: red; font-size: 1.2em; font-weight: bold;'>❌ Database Connection: FAILED</p>" +
                   "<p><strong>Error Details:</strong> " + e.getMessage() + "</p>" +
                   "<p>Check if your DB container is running and credentials are correct.</p>" +
                   "</body>" +
                   "</html>";
        }
    }
}
