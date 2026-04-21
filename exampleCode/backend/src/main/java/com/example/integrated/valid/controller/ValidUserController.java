package com.example.integrated.valid.controller;

import com.example.integrated.valid.dto.UserSignupDto;
import com.example.integrated.valid.entity.ValidUser;
import com.example.integrated.valid.repository.ValidUserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/valid/users")
@RequiredArgsConstructor
public class ValidUserController {

    private final ValidUserRepository validUserRepository;

    // ----------------------------------------
    // [시연] @Valid 없는 안전하지 않은 API (AS-IS)
    // 쓰레기 값이 그대로 DB에 저장됨!
    // ----------------------------------------
    @PostMapping("/signup-unsafe")
    public ResponseEntity<String> signupUnsafe(@RequestBody UserSignupDto dto) {
        ValidUser user = ValidUser.builder()
                .username(dto.getUsername())
                .password(dto.getPassword())
                .email(dto.getEmail())
                .age(dto.getAge())
                .build();

        validUserRepository.save(user);
        return ResponseEntity.ok("[위험] 저장 완료! H2 콘솔(http://localhost:8080/h2-console)에서 확인하세요. " +
                "이상한 값이 그대로 들어가 있습니다.");
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserSignupDto dto) {

        // 중복 아이디/이메일 체크
        if (validUserRepository.existsByUsername(dto.getUsername())) {
            return ResponseEntity.badRequest().body("이미 사용 중인 아이디입니다.");
        }
        if (validUserRepository.existsByEmail(dto.getEmail())) {
            return ResponseEntity.badRequest().body("이미 사용 중인 이메일입니다.");
        }

        ValidUser user = ValidUser.builder()
                .username(dto.getUsername())
                .password(dto.getPassword())
                .email(dto.getEmail())
                .age(dto.getAge())
                .build();

        ValidUser saved = validUserRepository.save(user);
        return ResponseEntity.ok("가입 완료! (ID: " + saved.getId() + ")" +
                " H2 콘솔에서 SELECT * FROM VALID_USERS 로 확인하세요!");
    }

    // 가입 목록 조회
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(validUserRepository.findAll());
    }
}
