package com.example.study.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 사용자 정보를 관리하는 엔티티 클래스.
 * 회원 기본 정보 및 소셜 로그인 연동 정보, 권한(Role) 등을 저장합니다.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_kakao_id", columnNames = "kakao_id"),
                @UniqueConstraint(name = "uk_users_naver_id", columnNames = "naver_id"),
                @UniqueConstraint(name = "uk_users_google_id", columnNames = "google_id"),
                @UniqueConstraint(name = "uk_users_username", columnNames = "username")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kakao_id")
    private Long kakaoId;

    @Column(name = "naver_id", length = 100)
    private String naverId;

    @Column(name = "google_id", length = 100)
    private String googleId;

    @Column(length = 50)
    private String username;

    @Column(length = 100)
    private String password;

    @Column(length = 100)
    private String nickname;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(length = 200)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "reset_token", length = 100)
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    @Column(name = "notification_opt_in", nullable = false)
    private boolean notificationOptIn = false;

    @Column(name = "kakao_access_token", length = 500)
    private String kakaoAccessToken;

    @Column(name = "kakao_refresh_token", length = 500)
    private String kakaoRefreshToken;

    @Column(name = "kakao_token_expires_at")
    private LocalDateTime kakaoTokenExpiresAt;

    @Column(name = "kakao_talk_opt_in", nullable = false)
    private boolean kakaoTalkOptIn = true;

    @Column(nullable = false, length = 20)
    private String role = "USER";

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getKakaoId() { return kakaoId; }
    public void setKakaoId(Long kakaoId) { this.kakaoId = kakaoId; }
    public String getNaverId() { return naverId; }
    public void setNaverId(String naverId) { this.naverId = naverId; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    public LocalDateTime getResetTokenExpiresAt() { return resetTokenExpiresAt; }
    public void setResetTokenExpiresAt(LocalDateTime resetTokenExpiresAt) { this.resetTokenExpiresAt = resetTokenExpiresAt; }
    public boolean isNotificationOptIn() { return notificationOptIn; }
    public void setNotificationOptIn(boolean notificationOptIn) { this.notificationOptIn = notificationOptIn; }
    public String getKakaoAccessToken() { return kakaoAccessToken; }
    public void setKakaoAccessToken(String kakaoAccessToken) { this.kakaoAccessToken = kakaoAccessToken; }
    public String getKakaoRefreshToken() { return kakaoRefreshToken; }
    public void setKakaoRefreshToken(String kakaoRefreshToken) { this.kakaoRefreshToken = kakaoRefreshToken; }
    public LocalDateTime getKakaoTokenExpiresAt() { return kakaoTokenExpiresAt; }
    public void setKakaoTokenExpiresAt(LocalDateTime kakaoTokenExpiresAt) { this.kakaoTokenExpiresAt = kakaoTokenExpiresAt; }
    public boolean isKakaoTalkOptIn() { return kakaoTalkOptIn; }
    public void setKakaoTalkOptIn(boolean kakaoTalkOptIn) { this.kakaoTalkOptIn = kakaoTalkOptIn; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    /** User 객체로부터 표시 이름 (닉네임 또는 사용자 이름)을 가져오는 헬퍼 메서드 */
    public String getDisplayName() {
        if (this.nickname != null && !this.nickname.isBlank()) {
            return this.nickname;
        }
        if (this.username != null && !this.username.isBlank()) {
            return this.username;
        }
        return "(알 수 없음)";
    }
}
