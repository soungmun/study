package com.example.study.controller;

import com.example.study.config.SecurityUser;
import com.example.study.dto.request.CommentRequest;
import com.example.study.dto.response.CommentResponse;
import com.example.study.dto.response.MessageResponse;
import com.example.study.service.CommentLikeService;
import com.example.study.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 공지사항 댓글 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * 댓글 작성, 수정, 삭제 및 좋아요 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/notices/{noticeId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;

    public CommentController(CommentService commentService, CommentLikeService commentLikeService) {
        this.commentService = commentService;
        this.commentLikeService = commentLikeService;
    }

    @GetMapping("")
    public ResponseEntity<List<CommentResponse>> list(@PathVariable Long noticeId,
                                                      @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(commentService.listByNotice(noticeId,
                principal != null ? principal.getUserId() : null));
    }

    @PostMapping("")
    public ResponseEntity<?> create(@PathVariable Long noticeId,
                                    @Valid @RequestBody CommentRequest req,
                                    @AuthenticationPrincipal SecurityUser principal) {
        if (principal == null) return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
        return ResponseEntity.ok(commentService.create(noticeId, principal.getUserId(), req.content()));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<?> update(@PathVariable Long commentId,
                                    @Valid @RequestBody CommentRequest req,
                                    @AuthenticationPrincipal SecurityUser principal) {
        if (principal == null) return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
        try {
            return ResponseEntity.ok(commentService.update(commentId, principal.getUserId(), req.content()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(MessageResponse.of(e.getMessage()));
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> delete(@PathVariable Long commentId,
                                    @AuthenticationPrincipal SecurityUser principal) {
        if (principal == null) return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
        try {
            commentService.delete(commentId, principal.getUserId());
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(MessageResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long commentId,
                                        @AuthenticationPrincipal SecurityUser principal) {
        if (principal == null) return ResponseEntity.status(401).body(MessageResponse.of("로그인이 필요합니다."));
        return ResponseEntity.ok(commentLikeService.toggle(commentId, principal.getUserId()));
    }
}
