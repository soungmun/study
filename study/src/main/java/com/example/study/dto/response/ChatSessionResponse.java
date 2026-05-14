package com.example.study.dto.response;

import com.example.study.entity.ChatSession;

import java.time.LocalDateTime;

public record ChatSessionResponse(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ChatSessionResponse from(ChatSession s) {
        return new ChatSessionResponse(s.getId(), s.getTitle(), s.getCreatedAt(), s.getUpdatedAt());
    }
}