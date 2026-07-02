package com.example.study.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // Setter 추가

import java.time.LocalDateTime;

/**
 * 공지사항 첨부 이미지 데이터를 관리하는 엔티티 클래스입니다.
 * 원본 파일명, 서버 저장 파일명 등을 저장합니다.
 */
@Entity
@Table(name = "notice_image",
        indexes = @Index(name = "ix_nimage_notice", columnList = "notice_id"))
@Getter
@Setter // notice 필드를 설정하기 위해 Setter 추가
@NoArgsConstructor
public class NoticeImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기존 noticeId 필드 제거

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

    // --- Notice 엔티티와의 ManyToOne 관계 추가 ---
    @ManyToOne(fetch = FetchType.LAZY) // NoticeImage는 하나의 Notice에 속함 (지연 로딩)
    @JoinColumn(name = "notice_id") // notice_id 컬럼이 Notice 엔티티의 ID를 참조
    private Notice notice; // 연관 관계의 주인 (외래키를 가짐)
    // ------------------------------------------

    public NoticeImage(Long userId, String storedName, String originalName,
                       long fileSize, String mimeType) {
        this.userId = userId;
        this.storedName = storedName;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        // notice 필드는 나중에 attachToNotice 등을 통해 설정
    }

    // Notice와 연결될 때 사용하는 생성자 (선택 사항)
    public NoticeImage(Long userId, String storedName, String originalName, long fileSize, String mimeType, Notice notice) {
        this(userId, storedName, originalName, fileSize, mimeType);
        this.notice = notice;
    }


    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}