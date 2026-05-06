package com.example.study.dto;

import com.example.study.entity.User;

public record UserResponse(
        Long id,
        Long kakaoId,
        String nickname,
        String profileImage,
        String email
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getKakaoId(),
                user.getNickname(),
                user.getProfileImage(),
                user.getEmail()
        );
    }
}