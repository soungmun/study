package com.example.study.controller;

import com.example.study.dto.KakaoTokenResponse;
import com.example.study.dto.KakaoUserResponse;
import com.example.study.dto.UserResponse;
import com.example.study.entity.User;
import com.example.study.repository.UserRepository;
import com.example.study.service.KakaoOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String SESSION_USER_KEY = "LOGIN_USER_ID";
    private static final String SESSION_RETURN_TO = "RETURN_TO";

    private final KakaoOAuthService kakao;
    private final UserRepository userRepository;
    private final String frontendUrl;

    public AuthController(
            KakaoOAuthService kakao,
            UserRepository userRepository,
            @Value("${app.frontend.url}") String frontendUrl
    ) {
        this.kakao = kakao;
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
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
            @RequestParam("code") String code,
            HttpSession session,
            HttpServletResponse res
    ) throws IOException {
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

        Object stored = session.getAttribute(SESSION_RETURN_TO);
        session.removeAttribute(SESSION_RETURN_TO);
        String target = (stored instanceof String s && !s.isBlank()) ? s : frontendUrl;
        res.sendRedirect(target);
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
}
