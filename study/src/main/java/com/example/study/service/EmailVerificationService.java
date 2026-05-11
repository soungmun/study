package com.example.study.service;

import com.example.study.entity.EmailVerification;
import com.example.study.repository.EmailVerificationRepository;
import com.example.study.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private static final int CODE_VALID_MINUTES = 5;
    private static final Duration RESEND_THROTTLE = Duration.ofSeconds(60);
    private static final Duration VERIFIED_VALIDITY = Duration.ofMinutes(30);

    private final EmailVerificationRepository repository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(EmailVerificationRepository repository,
                                    UserRepository userRepository,
                                    EmailService emailService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /** 인증번호 발송. 결과: { sent: true, expiresInSeconds } */
    @Transactional
    public SendResult sendCode(String rawEmail) {
        String email = normalize(rawEmail);
        if (email.isBlank()) throw new IllegalArgumentException("이메일을 입력해 주세요.");

        // 이미 가입된 이메일인지 — 보통 별도 안내가 좋지만 학습용으로 허용 (비번 찾기 흐름과 별개)
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다. 로그인 또는 비밀번호 찾기를 이용해 주세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<EmailVerification> existing = repository.findByEmail(email);

        // 60초 throttle
        existing.ifPresent(ev -> {
            if (ev.getSentAt() != null && Duration.between(ev.getSentAt(), now).compareTo(RESEND_THROTTLE) < 0) {
                long wait = RESEND_THROTTLE.minus(Duration.between(ev.getSentAt(), now)).toSeconds();
                throw new IllegalStateException(wait + "초 후 다시 시도해 주세요.");
            }
        });

        String code = randomCode();
        LocalDateTime expiresAt = now.plusMinutes(CODE_VALID_MINUTES);

        EmailVerification ev = existing.orElseGet(() -> new EmailVerification(email, code, expiresAt));
        if (existing.isPresent()) {
            ev.regenerate(code, expiresAt);
        }
        repository.save(ev);

        try {
            emailService.sendVerificationCode(email, code, CODE_VALID_MINUTES);
        } catch (MessagingException | UnsupportedEncodingException e) {
            // 발송 실패 시 row 도 지워서 사용자 재시도 가능하게
            repository.deleteByEmail(email);
            throw new IllegalStateException("이메일 발송에 실패했습니다. 주소를 다시 확인해 주세요. (" + e.getMessage() + ")");
        } catch (Exception e) {
            repository.deleteByEmail(email);
            log.warn("[EmailVer] 발송 실패 email={}: {}", email, e.getMessage());
            throw new IllegalStateException("이메일 발송에 실패했습니다. 주소를 다시 확인해 주세요.");
        }

        return new SendResult(true, CODE_VALID_MINUTES * 60L);
    }

    /** 코드 검증 → 성공 시 verifiedAt 마킹 */
    @Transactional
    public void verifyCode(String rawEmail, String code) {
        String email = normalize(rawEmail);
        EmailVerification ev = repository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("먼저 인증번호를 발송해 주세요."));
        if (ev.isExpired(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증번호가 만료되었습니다. 다시 발송해 주세요.");
        }
        if (code == null || !code.trim().equals(ev.getCode())) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }
        ev.markVerified();
    }

    /** 회원가입 시점에 호출 — 인증된 상태이고 30분 안인지 */
    public boolean isEmailVerified(String rawEmail) {
        String email = normalize(rawEmail);
        return repository.findByEmail(email)
                .map(ev -> ev.isVerified()
                        && Duration.between(ev.getVerifiedAt(), LocalDateTime.now()).compareTo(VERIFIED_VALIDITY) <= 0)
                .orElse(false);
    }

    /** 가입 완료 후 정리 */
    @Transactional
    public void consume(String rawEmail) {
        repository.deleteByEmail(normalize(rawEmail));
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private String randomCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    public record SendResult(boolean sent, long expiresInSeconds) {}
}
