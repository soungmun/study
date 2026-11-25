package com.example.study.service;

import lombok.extern.slf4j.Slf4j;
import com.example.study.entity.EmailVerification;
import com.example.study.repository.EmailVerificationRepository;
import com.example.study.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 회원가입 이메일 인증 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 인증번호 발송, 검증, 소비(완료 처리) 기능을 담당합니다.
 */
@Slf4j
@Service
public class EmailVerificationService {

    private static final int CODE_VALID_MINUTES = 5;
    private static final Duration RESEND_THROTTLE = Duration.ofSeconds(60);
    private static final Duration VERIFIED_VALIDITY = Duration.ofMinutes(30);

    private final EmailVerificationRepository repository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final MailboxVerifier mailboxVerifier;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(EmailVerificationRepository repository,
                                    UserRepository userRepository,
                                    EmailService emailService,
                                    MailboxVerifier mailboxVerifier) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.mailboxVerifier = mailboxVerifier;
    }

    /**
     * 이메일로 인증번호를 발송합니다.
     * 이미 가입된 이메일, 없는 메일 주소, 60초 내 재발송 요청 시 예외를 던집니다.
     *
     * @param rawEmail 인증번호를 받을 이메일 주소
     * @return 발송 결과 (sent: true, expiresInSeconds: 유효 시간)
     */
    @Transactional
    public SendResult sendCode(String rawEmail) {
        String email = normalize(rawEmail);
        if (email.isBlank()) throw new IllegalArgumentException("이메일을 입력해 주세요.");

        // 이미 가입된 이메일인지 — 보통 별도 안내가 좋지만 학습용으로 허용 (비번 찾기 흐름과 별개)
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다. 로그인 또는 비밀번호 찾기를 이용해 주세요.");
        }

        // 발송 전 메일함 존재 확인 — 없는 주소면 발송 자체를 막고 안내. (판단 불가 시엔 그대로 발송 진행)
        if (mailboxVerifier.verify(email) == MailboxVerifier.Result.UNDELIVERABLE) {
            throw new IllegalStateException("없는 메일 주소입니다. 이메일을 다시 확인해 주세요.");
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
            log.warn("[EmailVer] 발송 실패 email={}: {}", email, e.getMessage());
            throw new IllegalStateException("없는 메일 주소입니다. 이메일을 다시 확인해 주세요.");
        } catch (Exception e) {
            repository.deleteByEmail(email);
            log.warn("[EmailVer] 발송 실패 email={}: {}", email, e.getMessage());
            throw new IllegalStateException("없는 메일 주소입니다. 이메일을 다시 확인해 주세요.");
        }

        return new SendResult(true, CODE_VALID_MINUTES * 60L);
    }

    /**
     * 입력한 인증번호가 올바른지 검증하고, 성공 시 인증 완료 처리합니다.
     * 만료되었거나 코드가 불일치하면 예외를 던집니다.
     *
     * @param rawEmail 인증 대상 이메일
     * @param code     사용자가 입력한 인증번호
     */
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

    /**
     * 이메일 인증이 완료되었고 30분 이내인지 확인합니다.
     * 회원가입 직전 최종 유효성 검사에 사용합니다.
     *
     * @param rawEmail 확인할 이메일
     * @return 인증 완료 상태이면 true
     */
    public boolean isEmailVerified(String rawEmail) {
        String email = normalize(rawEmail);
        return repository.findByEmail(email)
                .map(ev -> ev.isVerified()
                        && Duration.between(ev.getVerifiedAt(), LocalDateTime.now()).compareTo(VERIFIED_VALIDITY) <= 0)
                .orElse(false);
    }

    /**
     * 회원가입 완료 후 인증 레코드를 삭제합니다.
     *
     * @param rawEmail 정리할 이메일
     */
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
