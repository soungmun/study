package com.example.study.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice")
@Getter
@Setter
@NoArgsConstructor
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "작성자를 입력하세요.")
    @Size(max = 100, message = "작성자는 100자 이하여야 합니다.")
    @Column(nullable = false, length = 100)
    private String author;

    @Column(name = "author_id")
    private Long authorId;

    @NotBlank(message = "제목을 입력하세요.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "내용을 입력하세요.")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "view_count", nullable = false)
    private long viewCount;

    public Notice(String author, String title, String content) {
        this.author = author;
        this.title = title;
        this.content = content;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void update(String author, String title, String content) {
        this.author = author;
        this.title = title;
        this.content = content;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public String getViewCountText() {
        if (viewCount >= 1000) {
            return String.format("%.1fk", viewCount / 1000.0);
        }
        return String.valueOf(viewCount);
    }
}