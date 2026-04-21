package com.example.integrated.valid.exception;

import lombok.Getter;
import java.util.List;

@Getter
public class ErrorResponse {

    private final int status;
    private final String message;
    private final List<FieldError> errors;

    public ErrorResponse(int status, String message, List<FieldError> errors) {
        this.status = status;
        this.message = message;
        this.errors = errors;
    }

    // 필드별 오류 정보를 담는 내부 클래스
    @Getter
    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
