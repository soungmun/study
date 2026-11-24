package com.example.study.service;

import com.example.study.dto.response.CommentResponse;
import com.example.study.entity.Comment;
import com.example.study.entity.User;
import com.example.study.repository.CommentRepository;
import com.example.study.repository.NoticeRepository;
import com.example.study.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 댓글 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 공지사항 게시글에 대한 댓글 작성, 수정, 삭제 및 조회 기능을 제공합니다.
 */
@Slf4j
@Service
@Transactional
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

    /**
     * 특정 게시글의 댓글 목록을 작성 시간 오름차순으로 반환합니다.
     * 작성자 닉네임, 좋아요 수, 본인 여부를 일괄 조회하여 함께 반환합니다.
     *
     * @param noticeId      댓글을 조회할 게시글 ID
     * @param currentUserId 현재 로그인한 사용자 ID (본인 여부, 좋아요 여부 판별용)
     * @return 댓글 응답 DTO 목록 (댓글이 없으면 빈 리스트)
     */
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

    /**
     * 게시글에 새 댓글을 작성합니다.
     * 게시글 존재 여부와 작성자 정보를 확인한 뒤 저장합니다.
     *
     * @param noticeId 댓글을 달 게시글 ID
     * @param userId   작성자 ID
     * @param content  댓글 내용
     * @return 저장된 댓글 응답 DTO (좋아요 0, 본인 댓글로 세팅)
     */
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

    /**
     * 댓글 내용을 수정합니다.
     * 해당 게시글의 댓글인지, 본인이 작성한 댓글인지 검증합니다.
     *
     * @param noticeId  댓글이 속한 게시글 ID
     * @param commentId 수정할 댓글 ID
     * @param userId    요청한 사용자 ID (본인 여부 검증용)
     * @param content   새로운 댓글 내용
     * @return 수정된 댓글 응답 DTO
     */
    @Transactional
    public CommentResponse update(Long noticeId, Long commentId, Long userId, String content) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!c.getNoticeId().equals(noticeId)) {
            throw new IllegalArgumentException("해당 게시글의 댓글이 아닙니다.");
        }
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

    /**
     * 댓글을 삭제합니다.
     * 해당 게시글의 댓글인지, 본인이 작성한 댓글인지 검증합니다.
     *
     * @param noticeId  댓글이 속한 게시글 ID
     * @param commentId 삭제할 댓글 ID
     * @param userId    요청한 사용자 ID (본인 여부 검증용)
     */
    @Transactional
    public void delete(Long noticeId, Long commentId, Long userId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!c.getNoticeId().equals(noticeId)) {
            throw new IllegalArgumentException("해당 게시글의 댓글이 아닙니다.");
        }
        if (!c.getUserId().equals(userId)) {
            throw new SecurityException("본인 댓글만 삭제할 수 있습니다.");
        }
        commentRepository.delete(c);
    }

    private String displayName(User u) {
        if (u == null) return "(탈퇴 회원)";
        if (u.getNickname() != null && !u.getNickname().isBlank()) return u.getNickname();
        return u.getUsername() != null ? u.getUsername() : "사용자";
    }
}