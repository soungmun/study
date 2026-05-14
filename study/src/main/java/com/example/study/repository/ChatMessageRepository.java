package com.example.study.repository;

import com.example.study.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByIdAsc(Long sessionId);
    void deleteBySessionId(Long sessionId);
}