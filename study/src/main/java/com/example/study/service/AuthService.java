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
import com.example.study.entity.Comment;
import com.example.study.entity.User;
import com.example.study.repository.CommentLikeRepository;
import com.example.study.repository.CommentRepository;
import com.example.study.repository.NoticeLikeRepository;
import com.example.study.repository.NoticeRepository;
import com.example.study.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 인증 및 계정 관리 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 소셜 로그인(카카오, 네이버, 구글) 연동 및 회원가입, 로그인 처리를 수행합니다.
 */
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
    private final NoticeRepository noticeRepository;
    private final CommentRepository commentRepository;
    private final NoticeLikeRepository noticeLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NoticeImageService noticeImageService;

    private final String frontendUrl;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            EmailService emailService,
            KakaoOAuthService kakaoOAuth,
            NaverOAuthService naverOAuth,
            GoogleOAuthService googleOAuth,
            EmailVerificationService emailVerificationService,
            NoticeRepository noticeRepository,
            CommentRepository commentRepository,
            NoticeLikeRepository noticeLikeRepository,
            CommentLikeRepository commentLikeRepository,
            NoticeImageService noticeImageService,
            PasswordEncoder passwordEncoder,
            @Value("${app.frontend.url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.kakaoOAuth = kakaoOAuth;
        this.naverOAuth = naverOAuth;
        this.googleOAuth = googleOAuth;
        this.emailVerificationService = emailVerificationService;
        this.noticeRepository = noticeRepository;
        this.commentRepository = commentRepository;
        this.noticeLikeRepository = noticeLikeRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.noticeImageService = noticeImageService;
        this.passwordEncoder = passwordEncoder;
        this.frontendUrl = frontendUrl;
    }

    /**
     * 신규 회원을 등록합니다.
     * 아이디 중복 확인 및 이메일 인증 완료 여부를 검증한 뒤 계정을 생성합니다.
     * 성공 시 환영 메일을 발송합니다.
     *
     * @param req 회원가입 요청 정보 (아이디, 비밀번호, 이메일, 닉네임 등)
     * @return OK(성공), USERNAME_TAKEN(아이디 중복), EMAIL_NOT_VERIFIED(이메일 미인증)
     */
    @Transactional
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
        emailService.sendWelcome(u.getEmail(), u.getDisplayName());
        return Result.ok(u);
    }

    /**
     * 아이디/비밀번호로 로그인합니다.
     *
     * @param req 로그인 요청 (아이디, 비밀번호)
     * @return 인증 성공 시 User, 실패 시 빈 Optional
     */
    public Optional<User> login(LoginRequest req) {
        return userRepository.findByUsername(req.username())
                .filter(u -> u.getPassword() != null
                        && passwordEncoder.matches(req.password(), u.getPassword()));
    }

    /**
     * 비밀번호 재설정 토큰을 발급하고 메일로 발송합니다.
     * 소셜 전용 계정(비밀번호 없는 계정)은 발급하지 않습니다.
     * 이메일이 존재하지 않아도 보안상 오류를 노출하지 않습니다.
     *
     * @param req 비밀번호 찾기 요청 (이메일)
     */
    @Transactional
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
            emailService.sendPasswordReset(u.getEmail(), u.getDisplayName(),
                    frontendUrl + "/reset?token=" + token);
        });
    }

    /**
     * 재설정 토큰으로 비밀번호를 변경합니다.
     * 토큰이 없거나 만료된 경우 INVALID_TOKEN을 반환합니다.
     *
     * @param req 비밀번호 재설정 요청 (토큰, 새 비밀번호)
     * @return OK(성공), INVALID_TOKEN(토큰 불일치 또는 만료)
     */
    @Transactional
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

    /**
     * 회원 프로필을 수정합니다. (닉네임, 이메일, 비밀번호, 알림 수신 동의)
     * 소셜 전용 계정은 이메일/비밀번호 변경이 불가합니다.
     * 비밀번호 변경 성공 시 변경 안내 메일을 발송합니다.
     *
     * @param userId 수정할 사용자 ID
     * @param req    수정 요청 정보
     * @return OK(성공), SOCIAL_LOCKED(소셜 전용 계정), CURRENT_PASSWORD_MISMATCH(현재 비밀번호 불일치), NOT_FOUND(사용자 없음)
     */
    @Transactional
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
                        emailService.sendPasswordChanged(u.getEmail(), u.getDisplayName());
                    }
                    return Result.ok(u);
                })
                .orElse(Result.fail(Result.Code.NOT_FOUND));
    }

    /**
     * 회원 탈퇴 처리를 수행합니다.
     * 작성 게시글 익명화 → 댓글 삭제 → 좋아요 삭제 → 이미지 삭제 → 계정 삭제 순으로 진행합니다.
     *
     * @param userId 탈퇴할 사용자 ID
     */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));

        // 1. 게시글 작성자 익명화
        noticeRepository.findByAuthorId(userId).forEach(notice -> {
            notice.setAuthorId(null);
            notice.setAuthor("(탈퇴 회원)");
            noticeRepository.save(notice);
        });

        // 2. 댓글 삭제 (userId 컬럼이 NOT NULL 이므로 삭제 처리)
        List<Comment> userComments = commentRepository.findByUserId(userId);
        commentRepository.deleteAll(userComments);

        // 3. 게시글 좋아요 삭제
        noticeLikeRepository.deleteByUserId(userId);

        // 4. 댓글 좋아요 삭제
        commentLikeRepository.deleteByUserId(userId);

        // 5. 업로드한 이미지 삭제 (파일 + DB)
        noticeImageService.deleteByUserId(userId);

        // 6. User 엔티티 삭제
        userRepository.delete(user);
    }

    /**
     * 카카오 인증 코드로 사용자를 조회하거나 신규 생성합니다.
     * 최초 로그인 시 카카오 프로필 정보로 계정을 자동 생성합니다.
     *
     * @param code 카카오 OAuth 인증 코드
     * @return 로그인 처리된 User 엔티티
     */
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

    /**
     * 네이버 인증 코드로 사용자를 조회하거나 신규 생성합니다.
     * 최초 로그인 시 네이버 프로필 정보로 계정을 자동 생성합니다.
     *
     * @param code  네이버 OAuth 인증 코드
     * @param state CSRF 방지용 state 값
     * @return 로그인 처리된 User 엔티티
     */
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

    /**
     * 구글 인증 코드로 사용자를 조회하거나 신규 생성합니다.
     * 최초 로그인 시 구글 프로필 정보로 계정을 자동 생성합니다.
     *
     * @param code 구글 OAuth 인증 코드
     * @return 로그인 처리된 User 엔티티
     */
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
}
