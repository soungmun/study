package com.example.study.dto.response;

import com.example.study.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        String role,
        String content,
        Integer inputTokens,
        Integer outputTokens,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(),
                m.getRole().name().toLowerCase(),
                m.getContent(),
                m.getInputTokens(),
                m.getOutputTokens(),
                m.getCreatedAt()
        );
    }
}