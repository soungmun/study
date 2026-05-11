package com.example.study.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.study.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long noticeId,
        Long authorId,
        String authorName,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updatedAt,
        boolean edited,
        boolean mine
) {
    public static CommentResponse of(Comment c, String authorName, boolean mine) {
        boolean edited = c.getUpdatedAt() != null
                && c.getCreatedAt() != null
                && c.getUpdatedAt().isAfter(c.getCreatedAt().plusSeconds(1));
        return new CommentResponse(
                c.getId(),
                c.getNoticeId(),
                c.getUserId(),
                authorName,
                c.getContent(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                edited,
                mine
        );
    }
}