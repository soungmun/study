package com.example.study.controller;

import com.example.study.dto.request.PasswordForgotRequest;
import com.example.study.dto.request.PasswordResetRequest;
import com.example.study.dto.request.SignupRequest;
import com.example.study.dto.request.UserUpdateRequest;
import com.example.study.dto.response.MessageResponse;
import com.example.study.dto.response.UserResponse;
import com.example.study.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class UserAccountController {

    private static final String DEFAULT_FORGOT_MESSAGE =
            "해당 이메일이 등록되어 있다면 재설정 링크를 발송했습니다.";

    private final AuthService authService;

    public UserAccountController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req, HttpSession session) {
        AuthService.Result result = authService.signup(req);
        return switch (result.code) {
            case OK -> {
                session.setAttribute(AuthController.SESSION_USER_KEY, result.user.getId());
                yield ResponseEntity.ok(UserResponse.from(result.user));
            }
            case USERNAME_TAKEN -> ResponseEntity.status(409)
                    .body(MessageResponse.of("이미 사용 중인 아이디입니다."));
            default -> ResponseEntity.status(400)
                    .body(MessageResponse.of("회원가입 처리에 실패했습니다."));
        };
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody PasswordForgotRequest req) {
        authService.issueResetToken(req);
        return ResponseEntity.ok(MessageResponse.of(DEFAULT_FORGOT_MESSAGE));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody PasswordResetRequest req) {
        AuthService.Result result = authService.resetPassword(req);
        return switch (result.code) {
            case OK -> ResponseEntity.ok(MessageResponse.of("비밀번호가 변경되었습니다."));
            default -> ResponseEntity.status(400)
                    .body(MessageResponse.of("유효하지 않거나 만료된 재설정 링크입니다."));
        };
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(
            @Valid @RequestBody UserUpdateRequest req,
            HttpSession session
    ) {
        Object id = session.getAttribute(AuthController.SESSION_USER_KEY);
        if (!(id instanceof Long userId)) {
            return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
        }
        AuthService.Result result = authService.updateProfile(userId, req);
        return switch (result.code) {
            case OK -> ResponseEntity.ok(UserResponse.from(result.user));
            case KAKAO_ONLY -> ResponseEntity.status(400)
                    .body(MessageResponse.of("카카오 로그인 계정은 비밀번호를 변경할 수 없습니다."));
            case CURRENT_PASSWORD_MISMATCH -> ResponseEntity.status(400)
                    .body(MessageResponse.of("현재 비밀번호가 일치하지 않습니다."));
            default -> ResponseEntity.status(401)
                    .body(MessageResponse.of("사용자를 찾을 수 없습니다."));
        };
    }
}