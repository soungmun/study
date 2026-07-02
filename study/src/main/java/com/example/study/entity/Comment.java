package com.example.study.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List; // List 추가

/**
 * 게시글의 댓글 데이터를 관리하는 엔티티 클래스입니다.
 * 작성자, 내용, 작성 시간 등을 저장합니다.
 */
@Entity
@Table(
        name = "notice_comment",
        indexes = @Index(name = "ix_comment_notice", columnList = "notice_id, created_at")
)
@Getter
@Setter
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notice_id", nullable = false)
    private Long noticeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- CommentLike와의 연관 관계 추가 ---
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CommentLike> likes = new ArrayList<>();
    // ------------------------------------

    public Comment(Long noticeId, Long userId, String content) {
        this.noticeId = noticeId;
        this.userId = userId;
        this.content = content;
    }

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

    // --- 편의 메서드 추가 (양방향 관계 설정 시 유용) ---
    public void addLike(CommentLike commentLike) {
        this.likes.add(commentLike);
        if (commentLike.getComment() != this) { // 무한 루프 방지
            commentLike.setComment(this);
        }
    }

    public void removeLike(CommentLike commentLike) {
        this.likes.remove(commentLike);
        if (commentLike.getComment() == this) { // 무한 루프 방지
            commentLike.setComment(null);
        }
    }
    // -------------------------------------------------
}
