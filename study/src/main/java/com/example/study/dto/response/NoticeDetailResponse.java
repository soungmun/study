package com.example.study.dto.response;

import com.example.study.entity.Notice;
import com.example.study.entity.User;
import com.example.study.service.NoticeImageService.ImageInfo;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeDetailResponse(
        Long id,
        String author,
        String nickname,
        String title,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        long viewCount,
        String viewCountText,
        long commentCount,
        long likeCount,
        boolean iLiked,
        boolean canEdit,
        List<String> imageUrls,
        List<ImageInfo> images
) {
    public static NoticeDetailResponse of(Notice n, User authorUser, long commentCount, long likeCount,
                                          boolean iLiked, boolean canEdit,
                                          List<String> imageUrls, List<ImageInfo> images) {
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
        return new NoticeDetailResponse(
                n.getId(),
                n.getAuthor(),
                displayNickname,
                n.getTitle(),
                n.getContent(),
                n.getCreatedAt(),
                n.getViewCount(),
                n.getViewCountText(),
                commentCount,
                likeCount,
                iLiked,
                canEdit,
                imageUrls,
                images
        );
    }
}
