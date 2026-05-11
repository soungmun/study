package com.example.study.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record LikedUserResponse(
        Long userId,
        String nickname,
        String profileImage,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime likedAt
) {}
