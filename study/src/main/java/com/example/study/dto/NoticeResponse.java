package com.example.study.dto;

import com.example.study.entity.Notice;

import java.time.LocalDateTime;

public class NoticeResponse {

    private final Long id;
    private final String author;
    private final String title;
    private final String content;
    private final LocalDateTime createdAt;

    public NoticeResponse(Notice notice) {
        this.id = notice.getId();
        this.author = notice.getAuthor();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.createdAt = notice.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}