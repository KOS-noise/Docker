package com.example.integrated.valid.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

public class GlobalExceptionHandler {

    // 적절한 예외 타입을 처리하는 핸들러 어노테이션을 추가하세요.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldError(
                        error.getField(),
                        error.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "입력값 유효성 검사에 실패했습니다.",
                fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }
}