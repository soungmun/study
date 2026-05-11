package com.example.study.dto.response;

import com.example.study.entity.Notice;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record NoticeListItem(
        Long id,
        String author,
        String title,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        long viewCount,
        String viewCountText,
        long commentCount,
        long likeCount
) {
    public static NoticeListItem of(Notice n, long commentCount, long likeCount) {
        return new NoticeListItem(
                n.getId(),
                n.getAuthor(),
                n.getTitle(),
                n.getContent(),
                n.getCreatedAt(),
                n.getViewCount(),
                n.getViewCountText(),
                commentCount,
                likeCount
        );
    }
}
