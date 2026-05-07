package com.example.study.service;

import com.example.study.dto.request.LoginRequest;
import com.example.study.dto.request.PasswordForgotRequest;
import com.example.study.dto.request.PasswordResetRequest;
import com.example.study.dto.request.SignupRequest;
import com.example.study.dto.request.UserUpdateRequest;
import com.example.study.dto.response.KakaoTokenResponse;
import com.example.study.dto.response.KakaoUserResponse;
import com.example.study.entity.User;
import com.example.study.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {

    public static final class Result {
        public enum Code { OK, USERNAME_TAKEN, INVALID_CREDENTIALS, INVALID_TOKEN, KAKAO_ONLY, CURRENT_PASSWORD_MISMATCH, NOT_FOUND }

        public final Code code;
        public final User user;

        private Result(Code code, User user) {
            this.code = code;
            this.user = user;
        }
        public static Result ok(User user) { return new Result(Code.OK, user); }
        public static Result fail(Code code) { return new Result(code, null); }
    }

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final KakaoOAuthService kakaoOAuth;
    private final String frontendUrl;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            EmailService emailService,
            KakaoOAuthService kakaoOAuth,
            @Value("${app.frontend.url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.kakaoOAuth = kakaoOAuth;
        this.frontendUrl = frontendUrl;
    }

    public Result signup(SignupRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            return Result.fail(Result.Code.USERNAME_TAKEN);
        }
        User u = new User();
        u.setUsername(req.username());
        u.setPassword(passwordEncoder.encode(req.password()));
        if (req.nickname() != null && !req.nickname().isBlank()) {
            u.setNickname(req.nickname());
        }
        u.setEmail(req.email().trim());
        userRepository.save(u);
        emailService.sendWelcome(u.getEmail(), displayName(u));
        return Result.ok(u);
    }

    public Optional<User> login(LoginRequest req) {
        return userRepository.findByUsername(req.username())
                .filter(u -> u.getPassword() != null
                        && passwordEncoder.matches(req.password(), u.getPassword()));
    }

    public void issueResetToken(PasswordForgotRequest req) {
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
            emailService.sendPasswordReset(u.getEmail(), displayName(u),
                    frontendUrl + "/reset?token=" + token);
        });
    }

    public Result resetPassword(PasswordResetRequest req) {
        return userRepository.findByResetToken(req.token())
                .filter(u -> u.getResetTokenExpiresAt() != null
                        && u.getResetTokenExpiresAt().isAfter(LocalDateTime.now()))
                .map(u -> {
                    u.setPassword(passwordEncoder.encode(req.newPassword()));
                    u.setResetToken(null);
                    u.setResetTokenExpiresAt(null);
                    userRepository.save(u);
                    return Result.ok(u);
                })
                .orElse(Result.fail(Result.Code.INVALID_TOKEN));
    }

    public Result updateProfile(Long userId, UserUpdateRequest req) {
        return userRepository.findById(userId)
                .map(u -> {
                    boolean passwordChanged = false;
                    if (req.nickname() != null && !req.nickname().isBlank()) {
                        u.setNickname(req.nickname().trim());
                    }
                    if (req.email() != null && !req.email().isBlank()) {
                        u.setEmail(req.email().trim());
                    }
                    if (req.newPassword() != null && !req.newPassword().isBlank()) {
                        if (u.getKakaoId() != null
                                && (u.getPassword() == null || u.getPassword().isBlank())) {
                            return Result.fail(Result.Code.KAKAO_ONLY);
                        }
                        if (u.getPassword() != null && !u.getPassword().isBlank()
                                && (req.currentPassword() == null
                                || !passwordEncoder.matches(req.currentPassword(), u.getPassword()))) {
                            return Result.fail(Result.Code.CURRENT_PASSWORD_MISMATCH);
                        }
                        u.setPassword(passwordEncoder.encode(req.newPassword()));
                        passwordChanged = true;
                    }
                    userRepository.save(u);
                    if (passwordChanged && u.getEmail() != null && !u.getEmail().isBlank()) {
                        emailService.sendPasswordChanged(u.getEmail(), displayName(u));
                    }
                    return Result.ok(u);
                })
                .orElse(Result.fail(Result.Code.NOT_FOUND));
    }

    public User syncKakaoUser(String code) {
        KakaoTokenResponse token = kakaoOAuth.exchangeCode(code);
        KakaoUserResponse me = kakaoOAuth.fetchUser(token.accessToken());

        User user = userRepository.findByKakaoId(me.id()).orElseGet(() -> {
            User created = new User();
            created.setKakaoId(me.id());
            if (me.kakaoAccount() != null) {
                created.setEmail(me.kakaoAccount().email());
                if (me.kakaoAccount().profile() != null) {
                    created.setNickname(me.kakaoAccount().profile().nickname());
                }
            }
            return created;
        });

        if (me.kakaoAccount() != null && me.kakaoAccount().profile() != null) {
            user.setProfileImage(me.kakaoAccount().profile().profileImageUrl());
        }
        userRepository.save(user);
        return user;
    }

    private static String displayName(User u) {
        return u.getNickname() != null ? u.getNickname() : u.getUsername();
    }
}