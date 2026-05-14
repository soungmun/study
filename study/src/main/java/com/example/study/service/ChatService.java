package com.example.study.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.example.study.entity.ChatMessage;
import com.example.study.entity.ChatSession;
import com.example.study.repository.ChatMessageRepository;
import com.example.study.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AnthropicClient anthropicClient;
    private final String model;
    private final long maxTokens;

    public ChatService(
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${anthropic.model:claude-opus-4-7}") String model,
            @Value("${anthropic.max-tokens:16000}") long maxTokens
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.model = model;
        this.maxTokens = maxTokens;
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Chat] ANTHROPIC_API_KEY 미설정 — /api/chat 호출 시 500 반환");
            this.anthropicClient = null;
        } else {
            this.anthropicClient = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        }
    }

    public List<ChatSession> listSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Optional<ChatSession> getSession(Long sessionId, Long userId) {
        return sessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId));
    }

    public List<ChatMessage> listMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByIdAsc(sessionId);
    }

    @Transactional
    public ChatSession createSession(Long userId, String title) {
        ChatSession s = new ChatSession();
        s.setUserId(userId);
        s.setTitle(title == null || title.isBlank() ? "새 대화" : title.trim());
        return sessionRepository.save(s);
    }

    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        sessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .ifPresent(s -> {
                    messageRepository.deleteBySessionId(sessionId);
                    sessionRepository.delete(s);
                });
    }

    @Transactional
    public ChatMessage sendMessage(Long sessionId, Long userId, String userContent) {
        if (anthropicClient == null) {
            throw new IllegalStateException("Anthropic API key가 설정되지 않았습니다. application-local.properties의 anthropic.api-key를 확인하세요.");
        }
        ChatSession session = sessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없거나 권한이 없습니다."));

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole(ChatMessage.Role.USER);
        userMsg.setContent(userContent);
        messageRepository.save(userMsg);

        List<ChatMessage> history = messageRepository.findBySessionIdOrderByIdAsc(sessionId);

        MessageCreateParams.Builder b = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .thinking(ThinkingConfigAdaptive.builder().build());
        for (ChatMessage m : history) {
            if (m.getRole() == ChatMessage.Role.USER) {
                b.addUserMessage(m.getContent());
            } else {
                b.addAssistantMessage(m.getContent());
            }
        }

        Message response;
        try {
            response = anthropicClient.messages().create(b.build());
        } catch (Exception e) {
            log.warn("[Chat] Anthropic 호출 실패 sessionId={}: {}", sessionId, e.getMessage());
            throw new IllegalStateException("Claude API 호출 실패: " + e.getMessage(), e);
        }

        StringBuilder text = new StringBuilder();
        response.content().forEach(block -> block.text().ifPresent(t -> text.append(t.text())));
        String assistantContent = text.toString().trim();
        if (assistantContent.isEmpty()) {
            assistantContent = "(응답 없음)";
        }

        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole(ChatMessage.Role.ASSISTANT);
        assistantMsg.setContent(assistantContent);
        assistantMsg.setInputTokens((int) response.usage().inputTokens());
        assistantMsg.setOutputTokens((int) response.usage().outputTokens());
        messageRepository.save(assistantMsg);

        if ("새 대화".equals(session.getTitle())) {
            String summary = userContent.length() > 40 ? userContent.substring(0, 40) + "…" : userContent;
            session.setTitle(summary);
        }
        sessionRepository.save(session);

        return assistantMsg;
    }
}