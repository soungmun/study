package com.example.study.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // Setter 추가

import java.time.LocalDateTime;

@Entity
@Table(
        name = "comment_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comment_like",
                columnNames = {"comment_id", "user_id"}
        ),
        indexes = @Index(name = "ix_clike_comment", columnList = "comment_id")
)
@Getter
@Setter // comment 필드를 설정하기 위해 Setter 추가
@NoArgsConstructor
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기존 commentId 필드 제거

    @ManyToOne(fetch = FetchType.LAZY) // CommentLike는 하나의 Comment에 속함 (지연 로딩)
    @JoinColumn(name = "comment_id", nullable = false) // comment_id 컬럼이 Comment 엔티티의 ID를 참조
    private Comment comment; // 연관 관계의 주인 (외래키를 가짐)

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CommentLike(Comment comment, Long userId) { // 생성자 변경
        this.comment = comment;
        this.userId = userId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
