package com.example.study.controller;

import com.example.study.dto.request.LoginRequest;
import com.example.study.dto.response.MessageResponse;
import com.example.study.dto.response.UserResponse;
import com.example.study.entity.User;
import com.example.study.repository.UserRepository;
import com.example.study.service.AuthService;
import com.example.study.service.KakaoOAuthService;
import com.example.study.service.NaverOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String SESSION_USER_KEY = "LOGIN_USER_ID";
    private static final String SESSION_RETURN_TO = "RETURN_TO";
    private static final String SESSION_KAKAO_LAST_CODE = "KAKAO_LAST_CODE";
    private static final String SESSION_NAVER_STATE = "NAVER_STATE";
    private static final String SESSION_NAVER_LAST_CODE = "NAVER_LAST_CODE";

    private final AuthService authService;
    private final KakaoOAuthService kakao;
    private final NaverOAuthService naver;
    private final UserRepository userRepository;
    private final String frontendUrl;
    private final SecureRandom random = new SecureRandom();

    public AuthController(
            AuthService authService,
            KakaoOAuthService kakao,
            NaverOAuthService naver,
            UserRepository userRepository,
            @Value("${app.frontend.url}") String frontendUrl
    ) {
        this.authService = authService;
        this.kakao = kakao;
        this.naver = naver;
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpSession session) {
        return authService.login(req)
                .map(u -> {
                    session.setAttribute(SESSION_USER_KEY, u.getId());
                    return ResponseEntity.ok((Object) UserResponse.from(u));
                })
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(MessageResponse.of("아이디 또는 비밀번호가 일치하지 않습니다.")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
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

        Object lastCode = session.getAttribute(SESSION_KAKAO_LAST_CODE);
        if (code.equals(lastCode)) {
            res.sendRedirect(base);
            return;
        }
        session.setAttribute(SESSION_KAKAO_LAST_CODE, code);

        try {
            User user = authService.syncKakaoUser(code);
            session.setAttribute(SESSION_USER_KEY, user.getId());
            res.sendRedirect(base);
        } catch (Exception e) {
            redirectWithError(res, base, "token_exchange_failed",
                    e.getMessage() != null ? e.getMessage() : "");
        }
    }

    @GetMapping("/naver/login")
    public void naverLogin(
            @RequestParam(name = "returnTo", required = false) String returnTo,
            HttpSession session,
            HttpServletResponse res
    ) throws IOException {
        if (returnTo != null && !returnTo.isBlank()) {
            session.setAttribute(SESSION_RETURN_TO, returnTo);
        }
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        session.setAttribute(SESSION_NAVER_STATE, state);
        res.sendRedirect(naver.authorizeUrl(state));
    }

    @GetMapping("/naver/callback")
    public void naverCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
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

        Object savedState = session.getAttribute(SESSION_NAVER_STATE);
        session.removeAttribute(SESSION_NAVER_STATE);
        if (!(savedState instanceof String ss) || state == null || !ss.equals(state)) {
            redirectWithError(res, base, "state_mismatch", "");
            return;
        }

        Object lastCode = session.getAttribute(SESSION_NAVER_LAST_CODE);
        if (code.equals(lastCode)) {
            res.sendRedirect(base);
            return;
        }
        session.setAttribute(SESSION_NAVER_LAST_CODE, code);

        try {
            User user = authService.syncNaverUser(code, state);
            session.setAttribute(SESSION_USER_KEY, user.getId());
            res.sendRedirect(base);
        } catch (Exception e) {
            redirectWithError(res, base, "naver_token_exchange_failed",
                    e.getMessage() != null ? e.getMessage() : "");
        }
    }

    private void redirectWithError(HttpServletResponse res, String base, String reason, String desc) throws IOException {
        String sep = base.contains("?") ? "&" : "?";
        res.sendRedirect(base + sep
                + "auth_error=" + URLEncoder.encode(reason, StandardCharsets.UTF_8)
                + "&auth_error_description=" + URLEncoder.encode(desc, StandardCharsets.UTF_8));
    }
}