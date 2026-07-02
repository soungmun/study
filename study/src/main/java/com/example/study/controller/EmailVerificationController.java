package com.example.study.controller;

import com.example.study.dto.response.MessageResponse;
import com.example.study.service.EmailVerificationService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이메일 인증 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * 회원가입 시 인증번호 발송 및 확인 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/auth/email")
public class EmailVerificationController {

    private final EmailVerificationService service;

    public EmailVerificationController(EmailVerificationService service) {
        this.service = service;
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody EmailReq req) {
        if (req == null || req.email() == null || req.email().isBlank()) {
            return ResponseEntity.status(400).body(MessageResponse.of("이메일을 입력해 주세요."));
        }
        try {
            return ResponseEntity.ok(service.sendCode(req.email()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(MessageResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody VerifyReq req) {
        if (req == null || req.email() == null || req.code() == null) {
            return ResponseEntity.status(400).body(MessageResponse.of("이메일과 인증번호를 입력해 주세요."));
        }
        try {
            service.verifyCode(req.email(), req.code());
            return ResponseEntity.ok(MessageResponse.of("인증되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(MessageResponse.of(e.getMessage()));
        }
    }

    public record EmailReq(
            @NotBlank @Email String email
    ) {}

    public record VerifyReq(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "6자리 숫자") String code
    ) {}
}
