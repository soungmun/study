package com.example.study.service;

import com.example.study.dto.request.LoginRequest;
import com.example.study.dto.request.PasswordForgotRequest;
import com.example.study.dto.request.PasswordResetRequest;
import com.example.study.dto.request.SignupRequest;
import com.example.study.dto.request.UserUpdateRequest;
import com.example.study.dto.response.GoogleTokenResponse;
import com.example.study.dto.response.GoogleUserResponse;
import com.example.study.dto.response.KakaoTokenResponse;
import com.example.study.dto.response.KakaoUserResponse;
import com.example.study.dto.response.NaverTokenResponse;
import com.example.study.dto.response.NaverUserResponse;
import com.example.study.entity.User;
import com.example.study.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public static final class Result {
        public enum Code { OK, USERNAME_TAKEN, INVALID_CREDENTIALS, INVALID_TOKEN, KAKAO_ONLY, SOCIAL_LOCKED, CURRENT_PASSWORD_MISMATCH, NOT_FOUND, EMAIL_NOT_VERIFIED }

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
    private final NaverOAuthService naverOAuth;
    private final GoogleOAuthService googleOAuth;
    private final EmailVerificationService emailVerificationService;
    private final String frontendUrl;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            EmailService emailService,
            KakaoOAuthService kakaoOAuth,
            NaverOAuthService naverOAuth,
            GoogleOAuthService googleOAuth,
            EmailVerificationService emailVerificationService,
            @Value("${app.frontend.url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.kakaoOAuth = kakaoOAuth;
        this.naverOAuth = naverOAuth;
        this.googleOAuth = googleOAuth;
        this.emailVerificationService = emailVerificationService;
        this.frontendUrl = frontendUrl;
    }

    public Result signup(SignupRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            return Result.fail(Result.Code.USERNAME_TAKEN);
        }
        if (!emailVerificationService.isEmailVerified(req.email())) {
            return Result.fail(Result.Code.EMAIL_NOT_VERIFIED);
        }
        User u = new User();
        u.setUsername(req.username());
        u.setPassword(passwordEncoder.encode(req.password()));
        if (req.nickname() != null && !req.nickname().isBlank()) {
            u.setNickname(req.nickname());
        }
        u.setEmail(req.email().trim());
        u.setNotificationOptIn(Boolean.TRUE.equals(req.notificationOptIn()));
        userRepository.save(u);
        emailVerificationService.consume(req.email());
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
                    boolean isSocialOnly = (u.getKakaoId() != null || u.getNaverId() != null || u.getGoogleId() != null)
                            && (u.getPassword() == null || u.getPassword().isBlank());
                    boolean emailRequested = req.email() != null && !req.email().isBlank()
                            && !req.email().trim().equals(u.getEmail());
                    boolean passwordRequested = req.newPassword() != null && !req.newPassword().isBlank();
                    if (isSocialOnly && (emailRequested || passwordRequested)) {
                        return Result.fail(Result.Code.SOCIAL_LOCKED);
                    }

                    boolean passwordChanged = false;
                    if (req.nickname() != null && !req.nickname().isBlank()) {
                        u.setNickname(req.nickname().trim());
                    }
                    if (req.email() != null && !req.email().isBlank()) {
                        u.setEmail(req.email().trim());
                    }
                    if (req.notificationOptIn() != null) {
                        u.setNotificationOptIn(req.notificationOptIn());
                    }
                    if (req.newPassword() != null && !req.newPassword().isBlank()) {
                        if ((u.getKakaoId() != null || u.getNaverId() != null || u.getGoogleId() != null)
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
        log.info("[KakaoLogin] code 수신 길이={} prefix={}",
                code == null ? 0 : code.length(),
                code == null ? "null" : code.substring(0, Math.min(8, code.length())));

        KakaoTokenResponse token;
        try {
            token = kakaoOAuth.exchangeCode(code);
        } catch (Exception e) {
            log.warn("[KakaoLogin] 토큰 교환 실패: {}", e.getMessage());
            throw e;
        }
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            log.warn("[KakaoLogin] 토큰 응답이 비어있거나 accessToken=null. token={}", token);
            throw new IllegalStateException("카카오 토큰 응답에 access token이 없습니다.");
        }
        String at = token.accessToken();
        log.info("[KakaoLogin] 토큰 OK type={} expiresIn={} atLen={} atPrefix={}",
                token.tokenType(), token.expiresIn(), at.length(),
                at.substring(0, Math.min(6, at.length())));

        KakaoUserResponse me;
        try {
            me = kakaoOAuth.fetchUser(at);
        } catch (Exception e) {
            log.warn("[KakaoLogin] 사용자 정보 조회 실패: {}", e.getMessage());
            throw e;
        }
        log.info("[KakaoLogin] 사용자 OK kakaoId={} hasAccount={} hasProfile={}",
                me == null ? null : me.id(),
                me != null && me.kakaoAccount() != null,
                me != null && me.kakaoAccount() != null && me.kakaoAccount().profile() != null);

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
        user.setKakaoAccessToken(at);
        if (token.expiresIn() != null) {
            user.setKakaoTokenExpiresAt(LocalDateTime.now().plusSeconds(token.expiresIn()));
        }
        if (token.refreshToken() != null && !token.refreshToken().isBlank()) {
            user.setKakaoRefreshToken(token.refreshToken());
        }
        userRepository.save(user);
        return user;
    }

    public User syncNaverUser(String code, String state) {
        log.info("[NaverLogin] code 수신 길이={} prefix={}",
                code == null ? 0 : code.length(),
                code == null ? "null" : code.substring(0, Math.min(8, code.length())));

        NaverTokenResponse token;
        try {
            token = naverOAuth.exchangeCode(code, state);
        } catch (Exception e) {
            log.warn("[NaverLogin] 토큰 교환 실패: {}", e.getMessage());
            throw e;
        }
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            log.warn("[NaverLogin] 토큰 응답이 비어있거나 accessToken=null. token={}", token);
            throw new IllegalStateException("네이버 토큰 응답에 access token이 없습니다.");
        }
        String at = token.accessToken();
        log.info("[NaverLogin] 토큰 OK type={} expiresIn={} atLen={}",
                token.tokenType(), token.expiresIn(), at.length());

        NaverUserResponse me;
        try {
            me = naverOAuth.fetchUser(at);
        } catch (Exception e) {
            log.warn("[NaverLogin] 사용자 정보 조회 실패: {}", e.getMessage());
            throw e;
        }
        if (me == null || me.response() == null || me.response().id() == null) {
            log.warn("[NaverLogin] 사용자 응답 비정상: {}", me);
            throw new IllegalStateException("네이버 사용자 정보를 가져올 수 없습니다.");
        }
        NaverUserResponse.NaverAccount account = me.response();
        log.info("[NaverLogin] 사용자 OK naverId={} hasEmail={} hasNickname={}",
                account.id(),
                account.email() != null,
                account.nickname() != null);

        User user = userRepository.findByNaverId(account.id()).orElseGet(() -> {
            User created = new User();
            created.setNaverId(account.id());
            created.setEmail(account.email());
            if (account.nickname() != null && !account.nickname().isBlank()) {
                created.setNickname(account.nickname());
            } else if (account.name() != null && !account.name().isBlank()) {
                created.setNickname(account.name());
            }
            return created;
        });

        if (account.profileImage() != null && !account.profileImage().isBlank()) {
            user.setProfileImage(account.profileImage());
        }
        userRepository.save(user);
        return user;
    }

    public User syncGoogleUser(String code) {
        log.info("[GoogleLogin] code 수신 길이={} prefix={}",
                code == null ? 0 : code.length(),
                code == null ? "null" : code.substring(0, Math.min(8, code.length())));

        GoogleTokenResponse token;
        try {
            token = googleOAuth.exchangeCode(code);
        } catch (Exception e) {
            log.warn("[GoogleLogin] 토큰 교환 실패: {}", e.getMessage());
            throw e;
        }
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            log.warn("[GoogleLogin] 토큰 응답이 비어있거나 accessToken=null. token={}", token);
            throw new IllegalStateException("구글 토큰 응답에 access token이 없습니다.");
        }
        String at = token.accessToken();
        log.info("[GoogleLogin] 토큰 OK type={} expiresIn={} atLen={}",
                token.tokenType(), token.expiresIn(), at.length());

        GoogleUserResponse me;
        try {
            me = googleOAuth.fetchUser(at);
        } catch (Exception e) {
            log.warn("[GoogleLogin] 사용자 정보 조회 실패: {}", e.getMessage());
            throw e;
        }
        if (me == null || me.sub() == null || me.sub().isBlank()) {
            log.warn("[GoogleLogin] 사용자 응답 비정상: {}", me);
            throw new IllegalStateException("구글 사용자 정보를 가져올 수 없습니다.");
        }
        log.info("[GoogleLogin] 사용자 OK googleId={} hasEmail={} hasName={}",
                me.sub(), me.email() != null, me.name() != null);

        User user = userRepository.findByGoogleId(me.sub()).orElseGet(() -> {
            User created = new User();
            created.setGoogleId(me.sub());
            created.setEmail(me.email());
            if (me.name() != null && !me.name().isBlank()) {
                created.setNickname(me.name());
            } else if (me.givenName() != null && !me.givenName().isBlank()) {
                created.setNickname(me.givenName());
            }
            return created;
        });

        if (me.picture() != null && !me.picture().isBlank()) {
            user.setProfileImage(me.picture());
        }
        userRepository.save(user);
        return user;
    }

    private static String displayName(User u) {
        return u.getNickname() != null ? u.getNickname() : u.getUsername();
    }
}