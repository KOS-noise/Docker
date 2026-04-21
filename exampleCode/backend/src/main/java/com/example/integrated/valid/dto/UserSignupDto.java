package com.example.integrated.valid.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;


@Getter
public class UserSignupDto {

    // TODO: username — 필수 입력, 영문 소문자와 숫자만 허용, 6~20자 제한
    private String username;

    // TODO: password — 필수 입력, 최소 8자 이상
    private String password;

    // TODO: email — 필수 입력, 이메일 형식 검증
    private String email;

    // TODO: age — 선택 입력(null 허용), 최솟값 제한 필요
    private Integer age;
}
