package com.example.study.dto.response;

import com.example.study.entity.Notice;
import com.example.study.entity.User; // User 엔티티 import 추가
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record NoticeListItem(
        Long id,
        String author, // 기존 author 필드는 유지 (혹시 모를 경우 대비)
        String nickname, // 닉네임 필드 추가
        String title,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        long viewCount,
        String viewCountText,
        long commentCount,
        long likeCount
) {
    // User 엔티티를 받아서 닉네임을 설정하도록 of 메서드 수정
    public static NoticeListItem of(Notice n, User authorUser, long commentCount, long likeCount) {
        String displayNickname = (authorUser != null && authorUser.getNickname() != null)
                                 ? authorUser.getNickname()
                                 : "(탈퇴 회원)"; // 탈퇴 회원의 경우 처리
        return new NoticeListItem(
                n.getId(),
                n.getAuthor(), // Notice 엔티티의 author 필드는 그대로 유지
                displayNickname, // User 엔티티의 닉네임을 사용
                n.getTitle(),
                n.getContent(),
                n.getCreatedAt(),
                n.getViewCount(),
                n.getViewCountText(),
                commentCount,
                likeCount
        );
    }

    // 기존 of 메서드도 오버로드하여 유지 (User 정보가 없을 때 사용)
    public static NoticeListItem of(Notice n, long commentCount, long likeCount) {
        return new NoticeListItem(
                n.getId(),
                n.getAuthor(),
                n.getAuthor(), // 닉네임 정보가 없을 경우 author를 닉네임으로 사용
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