package com.example.study.controller;

import com.example.study.dto.request.CommentRequest;
import com.example.study.dto.response.CommentResponse;
import com.example.study.dto.response.MessageResponse;
import com.example.study.service.CommentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/notices/{noticeId}/comments")
    public ResponseEntity<List<CommentResponse>> list(
            @PathVariable Long noticeId,
            HttpSession session
    ) {
        Long me = currentUserId(session);
        return ResponseEntity.ok(commentService.listByNotice(noticeId, me));
    }

    @PostMapping("/notices/{noticeId}/comments")
    public ResponseEntity<?> create(
            @PathVariable Long noticeId,
            @Valid @RequestBody CommentRequest req,
            HttpSession session
    ) {
        Long me = currentUserId(session);
        if (me == null) {
            return ResponseEntity.status(401)
                    .body(MessageResponse.of("로그인이 필요합니다."));
        }
        return ResponseEntity.ok(commentService.create(noticeId, me, req.content()));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<?> update(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest req,
            HttpSession session
    ) {
        Long me = currentUserId(session);
        if (me == null) {
            return ResponseEntity.status(401)
                    .body(MessageResponse.of("로그인이 필요합니다."));
        }
        try {
            return ResponseEntity.ok(commentService.update(commentId, me, req.content()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(MessageResponse.of(e.getMessage()));
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> delete(
            @PathVariable Long commentId,
            HttpSession session
    ) {
        Long me = currentUserId(session);
        if (me == null) {
            return ResponseEntity.status(401)
                    .body(MessageResponse.of("로그인이 필요합니다."));
        }
        try {
            commentService.delete(commentId, me);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(MessageResponse.of(e.getMessage()));
        }
    }

    private Long currentUserId(HttpSession session) {
        Object id = session.getAttribute(AuthController.SESSION_USER_KEY);
        return id instanceof Long u ? u : null;
    }
}
