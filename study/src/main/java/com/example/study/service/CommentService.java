package com.example.study.service;

import com.example.study.dto.response.CommentResponse;
import com.example.study.entity.Comment;
import com.example.study.entity.User;
import com.example.study.repository.CommentRepository;
import com.example.study.repository.NoticeRepository;
import com.example.study.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final CommentLikeService commentLikeService;

    public CommentService(CommentRepository commentRepository,
                          NoticeRepository noticeRepository,
                          UserRepository userRepository,
                          CommentLikeService commentLikeService) {
        this.commentRepository = commentRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.commentLikeService = commentLikeService;
    }

    public List<CommentResponse> listByNotice(Long noticeId, Long currentUserId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + noticeId);
        }
        List<Comment> comments = commentRepository.findByNoticeIdOrderByCreatedAtAsc(noticeId);
        if (comments.isEmpty()) return List.of();

        // 작성자 닉네임 일괄
        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, String> nameById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, this::displayName));

        // 댓글 좋아요 카운트 + 내가 누른 것 일괄
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        Map<Long, Long> likeCounts = commentLikeService.countsByCommentIds(commentIds);
        Set<Long> myLikes = commentLikeService.likedCommentIdsByUser(currentUserId, commentIds);

        return comments.stream()
                .map(c -> CommentResponse.of(
                        c,
                        nameById.getOrDefault(c.getUserId(), "(탈퇴 회원)"),
                        currentUserId != null && currentUserId.equals(c.getUserId()),
                        likeCounts.getOrDefault(c.getId(), 0L),
                        myLikes.contains(c.getId())
                ))
                .toList();
    }

    @Transactional
    public CommentResponse create(Long noticeId, Long userId, String content) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + noticeId);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Comment saved = commentRepository.save(new Comment(noticeId, userId, content.trim()));
        return CommentResponse.of(saved, displayName(user), true, 0L, false);
    }

    @Transactional
    public CommentResponse update(Long commentId, Long userId, String content) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!c.getUserId().equals(userId)) {
            throw new SecurityException("본인 댓글만 수정할 수 있습니다.");
        }
        c.setContent(content.trim());
        User user = userRepository.findById(userId).orElse(null);
        long likeCount = commentLikeService.countsByCommentIds(List.of(commentId))
                .getOrDefault(commentId, 0L);
        boolean iLiked = commentLikeService.likedCommentIdsByUser(userId, List.of(commentId))
                .contains(commentId);
        return CommentResponse.of(c, displayName(user), true, likeCount, iLiked);
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!c.getUserId().equals(userId)) {
            throw new SecurityException("본인 댓글만 삭제할 수 있습니다.");
        }
        commentRepository.delete(c);
    }

    public long count(Long noticeId) {
        return commentRepository.countByNoticeId(noticeId);
    }

    private String displayName(User u) {
        if (u == null) return "(탈퇴 회원)";
        if (u.getNickname() != null && !u.getNickname().isBlank()) return u.getNickname();
        return u.getUsername() != null ? u.getUsername() : "사용자";
    }
}