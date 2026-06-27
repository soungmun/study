package com.example.study.dto.response;

import com.example.study.entity.Notice;
import com.example.study.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record NoticeListItem(
        Long id,
        String author,
        String nickname,
        String title,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        long viewCount,
        String viewCountText,
        long commentCount,
        long likeCount
) {
    public static NoticeListItem of(Notice n, User authorUser, long commentCount, long likeCount) {
        String displayNickname;
        if (authorUser != null && authorUser.getUsername() != null && !authorUser.getUsername().isBlank()) {
            displayNickname = authorUser.getUsername();
        } else if (authorUser != null && authorUser.getNickname() != null && !authorUser.getNickname().isBlank()) {
            displayNickname = authorUser.getNickname();
        } else if (n.getAuthor() != null && !n.getAuthor().isBlank()) {
            displayNickname = n.getAuthor();
        } else {
            displayNickname = "(탈퇴 회원)";
        }
        return new NoticeListItem(
                n.getId(),
                n.getAuthor(),
                displayNickname,
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
