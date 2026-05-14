package com.example.study.controller;

import com.example.study.dto.request.ChatMessageRequest;
import com.example.study.dto.response.ChatMessageResponse;
import com.example.study.dto.response.ChatSessionResponse;
import com.example.study.dto.response.MessageResponse;
import com.example.study.service.ChatService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> listSessions(HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) return unauthorized();
        List<ChatSessionResponse> items = chatService.listSessions(userId).stream()
                .map(ChatSessionResponse::from)
                .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody(required = false) ChatSessionCreateRequest req, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) return unauthorized();
        String title = req == null ? null : req.title();
        return ResponseEntity.ok(ChatSessionResponse.from(chatService.createSession(userId, title)));
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<?> listMessages(@PathVariable("id") Long sessionId, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) return unauthorized();
        if (chatService.getSession(sessionId, userId).isEmpty()) {
            return ResponseEntity.status(404).body(MessageResponse.of("세션을 찾을 수 없습니다."));
        }
        List<ChatMessageResponse> items = chatService.listMessages(sessionId).stream()
                .map(ChatMessageResponse::from)
                .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable("id") Long sessionId,
            @Valid @RequestBody ChatMessageRequest req,
            HttpSession session
    ) {
        Long userId = currentUserId(session);
        if (userId == null) return unauthorized();
        try {
            return ResponseEntity.ok(ChatMessageResponse.from(
                    chatService.sendMessage(sessionId, userId, req.content())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(MessageResponse.of(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500).body(MessageResponse.of(e.getMessage()));
        }
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable("id") Long sessionId, HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) return unauthorized();
        chatService.deleteSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId(HttpSession session) {
        Object id = session.getAttribute(AuthController.SESSION_USER_KEY);
        return (id instanceof Long userId) ? userId : null;
    }

    private ResponseEntity<MessageResponse> unauthorized() {
        return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
    }

    public record ChatSessionCreateRequest(String title) {}
}