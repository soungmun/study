package com.example.study.controller;

import com.example.study.config.SecurityUser;
import com.example.study.dto.request.LoginRequest;
import com.example.study.dto.response.MessageResponse;
import com.example.study.dto.response.UserResponse;
import com.example.study.entity.User;
import com.example.study.repository.UserRepository;
import com.example.study.service.AuthService;
import com.example.study.service.GoogleOAuthService;
import com.example.study.service.KakaoOAuthService;
import com.example.study.service.NaverOAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 다른 컴포넌트가 참조하는 상수 — 유지
    public static final String SESSION_USER_KEY = "LOGIN_USER_ID";

    private static final String SESSION_RETURN_TO = "RETURN_TO";
    private static final String SESSION_KAKAO_LAST_CODE = "KAKAO_LAST_CODE";
    private static final String SESSION_NAVER_STATE = "NAVER_STATE";
    private static final String SESSION_NAVER_LAST_CODE = "NAVER_LAST_CODE";
    private static final String SESSION_GOOGLE_STATE = "GOOGLE_STATE";
    private static final String SESSION_GOOGLE_LAST_CODE = "GOOGLE_LAST_CODE";

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final KakaoOAuthService kakao;
    private final NaverOAuthService naver;
    private final GoogleOAuthService google;
    private final UserRepository userRepository;
    private final String frontendUrl;
    private final SecureRandom random = new SecureRandom();

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            KakaoOAuthService kakao,
            NaverOAuthService naver,
            GoogleOAuthService google,
            UserRepository userRepository,
            @Value("${app.frontend.url}") String frontendUrl
    ) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.kakao = kakao;
        this.naver = naver;
        this.google = google;
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
            User user = ((SecurityUser) auth.getPrincipal()).getUser();
            return ResponseEntity.ok(UserResponse.from(user));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(MessageResponse.of("아이디 또는 비밀번호가 일치하지 않습니다."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal SecurityUser principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        // 세션 캐시 대신 DB에서 최신 데이터 조회 (role 변경 등 즉시 반영)
        return userRepository.findById(principal.getUserId())
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.status(401).build());
    }

    // ──────────────── 카카오 ────────────────

    @GetMapping("/kakao/login")
    public void kakaoLogin(@RequestParam(required = false) String returnTo,
                           HttpSession session, HttpServletResponse res) throws IOException {
        if (returnTo != null && !returnTo.isBlank()) session.setAttribute(SESSION_RETURN_TO, returnTo);
        res.sendRedirect(kakao.authorizeUrl(null));
    }

    @GetMapping("/kakao/callback")
    public void kakaoCallback(@RequestParam(required = false) String code,
                              @RequestParam(required = false) String error,
                              @RequestParam(required = false) String error_description,
                              HttpServletRequest request, HttpServletResponse res) throws IOException {
        HttpSession session = request.getSession(false);
        String base = returnTo(session);

        if (error != null || code == null || code.isBlank()) {
            redirectError(res, base, error != null ? error : "missing_code",
                    error_description != null ? error_description : "");
            return;
        }
        Object lastCode = session != null ? session.getAttribute(SESSION_KAKAO_LAST_CODE) : null;
        if (code.equals(lastCode)) { res.sendRedirect(base); return; }
        if (session != null) session.setAttribute(SESSION_KAKAO_LAST_CODE, code);

        try {
            User user = authService.syncKakaoUser(code);
            loginAs(user, request);
            res.sendRedirect(base);
        } catch (Exception e) {
            redirectError(res, base, "token_exchange_failed",
                    e.getMessage() != null ? e.getMessage() : "");
        }
    }

    // ──────────────── 네이버 ────────────────

    @GetMapping("/naver/login")
    public void naverLogin(@RequestParam(required = false) String returnTo,
                           HttpSession session, HttpServletResponse res) throws IOException {
        if (returnTo != null && !returnTo.isBlank()) session.setAttribute(SESSION_RETURN_TO, returnTo);
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        session.setAttribute(SESSION_NAVER_STATE, state);
        res.sendRedirect(naver.authorizeUrl(state));
    }

    @GetMapping("/naver/callback")
    public void naverCallback(@RequestParam(required = false) String code,
                              @RequestParam(required = false) String state,
                              @RequestParam(required = false) String error,
                              @RequestParam(required = false) String error_description,
                              HttpServletRequest request, HttpServletResponse res) throws IOException {
        HttpSession session = request.getSession(false);
        String base = returnTo(session);

        if (error != null || code == null || code.isBlank()) {
            redirectError(res, base, error != null ? error : "missing_code",
                    error_description != null ? error_description : "");
            return;
        }
        Object savedState = session != null ? session.getAttribute(SESSION_NAVER_STATE) : null;
        if (session != null) session.removeAttribute(SESSION_NAVER_STATE);
        if (!(savedState instanceof String ss) || !ss.equals(state)) {
            redirectError(res, base, "state_mismatch", ""); return;
        }
        Object lastCode = session.getAttribute(SESSION_NAVER_LAST_CODE);
        if (code.equals(lastCode)) { res.sendRedirect(base); return; }
        session.setAttribute(SESSION_NAVER_LAST_CODE, code);

        try {
            User user = authService.syncNaverUser(code, state);
            loginAs(user, request);
            res.sendRedirect(base);
        } catch (Exception e) {
            redirectError(res, base, "naver_token_exchange_failed",
                    e.getMessage() != null ? e.getMessage() : "");
        }
    }

    // ──────────────── 구글 ────────────────

    @GetMapping("/google/login")
    public void googleLogin(@RequestParam(required = false) String returnTo,
                            HttpSession session, HttpServletResponse res) throws IOException {
        if (returnTo != null && !returnTo.isBlank()) session.setAttribute(SESSION_RETURN_TO, returnTo);
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        session.setAttribute(SESSION_GOOGLE_STATE, state);
        res.sendRedirect(google.authorizeUrl(state));
    }

    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam(required = false) String code,
                               @RequestParam(required = false) String state,
                               @RequestParam(required = false) String error,
                               @RequestParam(required = false) String error_description,
                               HttpServletRequest request, HttpServletResponse res) throws IOException {
        HttpSession session = request.getSession(false);
        String base = returnTo(session);

        if (error != null || code == null || code.isBlank()) {
            redirectError(res, base, error != null ? error : "missing_code",
                    error_description != null ? error_description : "");
            return;
        }
        Object savedState = session != null ? session.getAttribute(SESSION_GOOGLE_STATE) : null;
        if (session != null) session.removeAttribute(SESSION_GOOGLE_STATE);
        if (!(savedState instanceof String ss) || !ss.equals(state)) {
            redirectError(res, base, "state_mismatch", ""); return;
        }
        Object lastCode = session != null ? session.getAttribute(SESSION_GOOGLE_LAST_CODE) : null;
        if (code.equals(lastCode)) { res.sendRedirect(base); return; }
        if (session != null) session.setAttribute(SESSION_GOOGLE_LAST_CODE, code);

        try {
            User user = authService.syncGoogleUser(code);
            loginAs(user, request);
            res.sendRedirect(base);
        } catch (Exception e) {
            redirectError(res, base, "google_token_exchange_failed",
                    e.getMessage() != null ? e.getMessage() : "");
        }
    }

    // ──────────────── 공통 헬퍼 ────────────────

    /** 소셜 로그인 성공 시 SecurityContext 세션에 저장 */
    private void loginAs(User user, HttpServletRequest request) {
        SecurityUser principal = new SecurityUser(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
    }

    private String returnTo(HttpSession session) {
        if (session == null) return frontendUrl;
        Object stored = session.getAttribute(SESSION_RETURN_TO);
        session.removeAttribute(SESSION_RETURN_TO);
        return (stored instanceof String s && !s.isBlank()) ? s : frontendUrl;
    }

    private void redirectError(HttpServletResponse res, String base,
                               String reason, String desc) throws IOException {
        String sep = base.contains("?") ? "&" : "?";
        res.sendRedirect(base + sep
                + "auth_error=" + URLEncoder.encode(reason, StandardCharsets.UTF_8)
                + "&auth_error_description=" + URLEncoder.encode(desc, StandardCharsets.UTF_8));
    }
}
