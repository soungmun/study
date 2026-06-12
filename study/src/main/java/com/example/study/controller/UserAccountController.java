package com.example.study.controller;

import com.example.study.config.SecurityUser;
import com.example.study.dto.request.PasswordForgotRequest;
import com.example.study.dto.request.PasswordResetRequest;
import com.example.study.dto.request.SignupRequest;
import com.example.study.dto.request.UserUpdateRequest;
import com.example.study.dto.response.MessageResponse;
import com.example.study.dto.response.UserResponse;
import com.example.study.repository.UserRepository;
import com.example.study.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class UserAccountController {

    private static final String DEFAULT_FORGOT_MESSAGE =
            "해당 이메일이 등록되어 있다면 재설정 링크를 발송했습니다.";

    private final AuthService authService;
    private final UserRepository userRepository;

    public UserAccountController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req, HttpServletRequest request) {
        AuthService.Result result = authService.signup(req);
        return switch (result.code) {
            case OK -> {
                SecurityUser principal = new SecurityUser(result.user);
                var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                ctx.setAuthentication(auth);
                SecurityContextHolder.setContext(ctx);
                HttpSession session = request.getSession(true);
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
                yield ResponseEntity.ok(UserResponse.from(result.user));
            }
            case USERNAME_TAKEN -> ResponseEntity.status(409).body(MessageResponse.of("이미 사용 중인 아이디입니다."));
            case EMAIL_NOT_VERIFIED -> ResponseEntity.status(400).body(MessageResponse.of("이메일 인증을 완료해 주세요."));
            default -> ResponseEntity.status(400).body(MessageResponse.of("회원가입 처리에 실패했습니다."));
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
            default -> ResponseEntity.status(400).body(MessageResponse.of("유효하지 않거나 만료된 재설정 링크입니다."));
        };
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<?> checkNickname(@RequestParam String nickname,
                                           @AuthenticationPrincipal SecurityUser principal) {
        String trimmed = nickname == null ? "" : nickname.trim();
        if (trimmed.isEmpty() || trimmed.length() > 50) {
            return ResponseEntity.badRequest().body(MessageResponse.of("닉네임은 1~50자여야 합니다."));
        }
        boolean taken;
        if (principal != null) {
            taken = userRepository.existsByNicknameAndIdNot(trimmed, principal.getUserId());
        } else {
            taken = userRepository.existsByNickname(trimmed);
        }
        if (taken) {
            return ResponseEntity.status(409).body(MessageResponse.of("이미 사용 중인 닉네임입니다."));
        }
        return ResponseEntity.ok(MessageResponse.of("사용 가능한 닉네임입니다."));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@Valid @RequestBody UserUpdateRequest req,
                                      @AuthenticationPrincipal SecurityUser principal) {
        if (principal == null) return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
        AuthService.Result result = authService.updateProfile(principal.getUserId(), req);
        return switch (result.code) {
            case OK -> ResponseEntity.ok(UserResponse.from(result.user));
            case KAKAO_ONLY -> ResponseEntity.status(400).body(MessageResponse.of("간편 로그인 계정은 비밀번호를 변경할 수 없습니다."));
            case SOCIAL_LOCKED -> ResponseEntity.status(400).body(MessageResponse.of("간편 로그인 계정은 이메일과 비밀번호를 변경할 수 없습니다."));
            case CURRENT_PASSWORD_MISMATCH -> ResponseEntity.status(400).body(MessageResponse.of("현재 비밀번호가 일치하지 않습니다."));
            default -> ResponseEntity.status(401).body(MessageResponse.of("사용자를 찾을 수 없습니다."));
        };
    }
}
