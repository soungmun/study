package com.example.study.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notice_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notice_like",
                columnNames = {"notice_id", "user_id"}
        ),
        indexes = @Index(name = "ix_like_notice", columnList = "notice_id")
)
@Getter
@NoArgsConstructor
public class NoticeLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notice_id", nullable = false)
    private Long noticeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public NoticeLike(Long noticeId, Long userId) {
        this.noticeId = noticeId;
        this.userId = userId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}