package com.example.study.dto.response;

import com.example.study.entity.User;

import java.time.LocalDateTime;

public record UserListItem(
        Long id,
        String username,
        String nickname,
        Long kakaoId,
        String naverId,
        String googleId,
        String email,
        LocalDateTime createdAt
) {
    public static UserListItem from(User u) {
        return new UserListItem(
                u.getId(),
                u.getUsername(),
                u.getNickname(),
                u.getKakaoId(),
                u.getNaverId(),
                u.getGoogleId(),
                u.getEmail(),
                u.getCreatedAt()
        );
    }
}