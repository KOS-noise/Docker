package com.example.integrated.cors.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/cors")
public class GreetingController {

    @GetMapping("/greeting")
    public String greeting() {
        return "안녕하세요! [GET] 백엔드에서 텍스트를 성공적으로 가져왔습니다.";
    }

    // JSON 본문을 받아오는 POST 처리 (Preflight Request 발생 유도)
    @PostMapping("/data")
    public String receiveData(@RequestBody Map<String, String> data) {
        String msg = data.get("message");
        return "성공! [POST] 클라이언트로부터 받은 메시지: " + msg;
    }
}
