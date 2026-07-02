package com.example.study.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 이메일 인증 정보를 관리하는 엔티티 클래스입니다.
 * 발송된 인증 코드와 만료 시간, 인증 성공 여부 등을 저장합니다.
 */
@Entity
@Table(
        name = "email_verification",
        uniqueConstraints = @UniqueConstraint(name = "uk_email_ver_email", columnNames = "email")
)
@Getter
@Setter
@NoArgsConstructor
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    public EmailVerification(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
        this.sentAt = LocalDateTime.now();
    }

    /** 새 코드 발급으로 갱신 (인증 상태 초기화) */
    public void regenerate(String code, LocalDateTime expiresAt) {
        this.code = code;
        this.expiresAt = expiresAt;
        this.sentAt = LocalDateTime.now();
        this.verifiedAt = null;
    }

    public void markVerified() {
        this.verifiedAt = LocalDateTime.now();
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }
}
