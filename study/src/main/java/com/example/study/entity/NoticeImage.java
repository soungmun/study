package com.example.study.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice_image",
        indexes = @Index(name = "ix_nimage_notice", columnList = "notice_id"))
@Getter
@NoArgsConstructor
public class NoticeImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연결된 게시글 ID. 업로드 직후에는 null(미연결 상태). */
    @Column(name = "notice_id")
    private Long noticeId;

    /** 업로드한 사용자 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 서버에 저장된 파일명 (UUID 기반) */
    @Column(name = "stored_name", nullable = false, length = 200)
    private String storedName;

    /** 원본 파일명 */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** 파일 크기 (bytes) */
    @Column(name = "file_size", nullable = false)
    private long fileSize;

    /** MIME 타입 (예: image/jpeg) */
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public NoticeImage(Long userId, String storedName, String originalName,
                       long fileSize, String mimeType) {
        this.userId = userId;
        this.storedName = storedName;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** 게시글 저장 후 이미지를 게시글에 연결할 때 사용 */
    public void attachToNotice(Long noticeId) {
        this.noticeId = noticeId;
    }
}
