package com.example.study.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // Setter 추가

import java.time.LocalDateTime;

/**
 * 공지사항 게시글 좋아요 데이터를 관리하는 엔티티 클래스입니다.
 * 어떤 사용자가 어떤 게시글에 좋아요를 눌렀는지 기록합니다.
 */
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
@Setter // notice 필드를 설정하기 위해 Setter 추가
@NoArgsConstructor
public class NoticeLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기존 noticeId 필드 제거

    @ManyToOne(fetch = FetchType.LAZY) // NoticeLike는 하나의 Notice에 속함 (지연 로딩)
    @JoinColumn(name = "notice_id", nullable = false) // notice_id 컬럼이 Notice 엔티티의 ID를 참조
    private Notice notice; // 연관 관계의 주인 (외래키를 가짐)

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public NoticeLike(Notice notice, Long userId) { // 생성자 변경
        this.notice = notice;
        this.userId = userId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
