package com.example.study.controller;

import com.example.study.dto.KakaoTokenResponse;
import com.example.study.dto.KakaoUserResponse;
import com.example.study.dto.LoginRequest;
import com.example.study.dto.PasswordForgotRequest;
import com.example.study.dto.PasswordResetRequest;
import com.example.study.dto.SignupRequest;
import com.example.study.dto.UserResponse;
import com.example.study.dto.UserUpdateRequest;
import com.example.study.entity.User;
import com.example.study.repository.UserRepository;
import com.example.study.service.EmailService;
import com.example.study.service.KakaoOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String SESSION_USER_KEY = "LOGIN_USER_ID";
    private static final String SESSION_RETURN_TO = "RETURN_TO";

    private final KakaoOAuthService kakao;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final String frontendUrl;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthController(
            KakaoOAuthService kakao,
            UserRepository userRepository,
            EmailService emailService,
            @Value("${app.frontend.url}") String frontendUrl
    ) {
        this.kakao = kakao;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req, HttpSession session) {
        if (userRepository.existsByUsername(req.username())) {
            return ResponseEntity.status(409).body(Map.of("message", "이미 사용 중인 아이디입니다."));
        }
        User u = new User();
        u.setUsername(req.username());
        u.setPassword(passwordEncoder.encode(req.password()));
        if (req.nickname() != null && !req.nickname().isBlank()) {
            u.setNickname(req.nickname());
        }
        u.setEmail(req.email().trim());
        userRepository.save(u);
        session.setAttribute(SESSION_USER_KEY, u.getId());
        emailService.sendWelcome(u.getEmail(),
                u.getNickname() != null ? u.getNickname() : u.getUsername());
        return ResponseEntity.ok(UserResponse.from(u));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody PasswordForgotRequest req) {
        userRepository.findByEmail(req.email().trim()).ifPresent(u -> {
            if (u.getPassword() == null || u.getPassword().isBlank()) {
                return;
            }
            byte[] buf = new byte[32];
            random.nextBytes(buf);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
            u.setResetToken(token);
            u.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
            userRepository.save(u);
            String resetUrl = frontendUrl + "/reset?token=" + token;
            emailService.sendPasswordReset(u.getEmail(),
                    u.getNickname() != null ? u.getNickname() : u.getUsername(),
                    resetUrl);
        });
        return ResponseEntity.ok(Map.of("message",
                "해당 이메일이 등록되어 있다면 재설정 링크를 발송했습니다."));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest req) {
        return userRepository.findByResetToken(req.token())
                .filter(u -> u.getResetTokenExpiresAt() != null
                        && u.getResetTokenExpiresAt().isAfter(LocalDateTime.now()))
                .map(u -> {
                    u.setPassword(passwordEncoder.encode(req.newPassword()));
                    u.setResetToken(null);
                    u.setResetTokenExpiresAt(null);
                    userRepository.save(u);
                    return ResponseEntity.ok((Object) Map.of("message", "비밀번호가 변경되었습니다."));
                })
                .orElseGet(() -> ResponseEntity.status(400).body(
                        Map.of("message", "유효하지 않거나 만료된 재설정 링크입니다.")));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpSession session) {
        return userRepository.findByUsername(req.username())
                .filter(u -> u.getPassword() != null && passwordEncoder.matches(req.password(), u.getPassword()))
                .map(u -> {
                    session.setAttribute(SESSION_USER_KEY, u.getId());
                    return ResponseEntity.ok((Object) UserResponse.from(u));
                })
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "아이디 또는 비밀번호가 일치하지 않습니다.")));
    }

    @GetMapping("/kakao/login")
    public void kakaoLogin(
            @RequestParam(name = "returnTo", required = false) String returnTo,
            HttpSession session,
            HttpServletResponse res
    ) throws IOException {
        if (returnTo != null && !returnTo.isBlank()) {
            session.setAttribute(SESSION_RETURN_TO, returnTo);
        }
        res.sendRedirect(kakao.authorizeUrl(null));
    }

    @GetMapping("/kakao/callback")
    public void kakaoCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            HttpSession session,
            HttpServletResponse res
    ) throws IOException {
        Object stored = session.getAttribute(SESSION_RETURN_TO);
        session.removeAttribute(SESSION_RETURN_TO);
        String base = (stored instanceof String s && !s.isBlank()) ? s : frontendUrl;

        if (error != null || code == null || code.isBlank()) {
            String reason = error != null ? error : "missing_code";
            String desc = errorDescription != null ? errorDescription : "";
            redirectWithError(res, base, reason, desc);
            return;
        }

        try {
            KakaoTokenResponse token = kakao.exchangeCode(code);
            KakaoUserResponse me = kakao.fetchUser(token.accessToken());

            User user = userRepository.findByKakaoId(me.id()).orElseGet(User::new);
            user.setKakaoId(me.id());
            if (me.kakaoAccount() != null) {
                user.setEmail(me.kakaoAccount().email());
                if (me.kakaoAccount().profile() != null) {
                    user.setNickname(me.kakaoAccount().profile().nickname());
                    user.setProfileImage(me.kakaoAccount().profile().profileImageUrl());
                }
            }
            userRepository.save(user);
            session.setAttribute(SESSION_USER_KEY, user.getId());
            res.sendRedirect(base);
        } catch (Exception e) {
            redirectWithError(res, base, "token_exchange_failed", e.getMessage() != null ? e.getMessage() : "");
        }
    }

    private void redirectWithError(HttpServletResponse res, String base, String reason, String desc) throws IOException {
        String sep = base.contains("?") ? "&" : "?";
        res.sendRedirect(base + sep
                + "auth_error=" + URLEncoder.encode(reason, StandardCharsets.UTF_8)
                + "&auth_error_description=" + URLEncoder.encode(desc, StandardCharsets.UTF_8));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_KEY);
        if (!(id instanceof Long userId)) {
            return ResponseEntity.status(401).build();
        }
        return userRepository.findById(userId)
                .map(u -> ResponseEntity.ok(UserResponse.from(u)))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(
            @Valid @RequestBody UserUpdateRequest req,
            HttpSession session
    ) {
        Object id = session.getAttribute(SESSION_USER_KEY);
        if (!(id instanceof Long userId)) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return userRepository.findById(userId)
                .map(u -> {
                    if (req.nickname() != null && !req.nickname().isBlank()) {
                        u.setNickname(req.nickname().trim());
                    }
                    if (req.email() != null && !req.email().isBlank()) {
                        u.setEmail(req.email().trim());
                    }
                    if (req.newPassword() != null && !req.newPassword().isBlank()) {
                        if (u.getKakaoId() != null && (u.getPassword() == null || u.getPassword().isBlank())) {
                            return ResponseEntity.status(400).body((Object) Map.of(
                                    "message", "카카오 로그인 계정은 비밀번호를 변경할 수 없습니다."));
                        }
                        if (u.getPassword() != null && !u.getPassword().isBlank()) {
                            if (req.currentPassword() == null
                                    || !passwordEncoder.matches(req.currentPassword(), u.getPassword())) {
                                return ResponseEntity.status(400).body((Object) Map.of(
                                        "message", "현재 비밀번호가 일치하지 않습니다."));
                            }
                        }
                        u.setPassword(passwordEncoder.encode(req.newPassword()));
                    }
                    userRepository.save(u);
                    return ResponseEntity.ok((Object) UserResponse.from(u));
                })
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "사용자를 찾을 수 없습니다.")));
    }
}
