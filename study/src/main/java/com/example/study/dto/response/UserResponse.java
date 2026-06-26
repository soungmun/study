package com.example.study.dto.response;

import com.example.study.entity.User;

public record UserResponse(
        Long id,
        Long kakaoId,
        String naverId,
        String googleId,
        String username,
        String nickname,
        String profileImage,
        String email,
        boolean notificationOptIn,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getKakaoId(),
                user.getNaverId(),
                user.getGoogleId(),
                user.getUsername(),
                user.getNickname(),
                user.getProfileImage(),
                user.getEmail(),
                user.isNotificationOptIn(),
                user.getRole()
        );
    }
}
