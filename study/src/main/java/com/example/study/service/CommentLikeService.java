package com.example.study.service;

import lombok.extern.slf4j.Slf4j;
import com.example.study.entity.Comment;
import com.example.study.entity.CommentLike;
import com.example.study.entity.Notice;
import com.example.study.entity.User;
import com.example.study.repository.CommentLikeRepository;
import com.example.study.repository.CommentRepository;
import com.example.study.repository.NoticeRepository;
import com.example.study.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 댓글 좋아요 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 특정 사용자의 댓글 좋아요 추가 및 취소(토글) 기능을 수행합니다.
 */
@Slf4j
@Service
@Transactional
public class CommentLikeService {

    private final CommentLikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final String frontendUrl;

    public CommentLikeService(CommentLikeRepository likeRepository,
                              CommentRepository commentRepository,
                              NoticeRepository noticeRepository,
                              UserRepository userRepository,
                              EmailService emailService,
                              @Value("${app.frontend.url}") String frontendUrl) {
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    /**
     * 댓글 좋아요를 토글합니다.
     * 이미 좋아요를 눌렀으면 취소, 누르지 않았으면 추가합니다.
     * 좋아요 추가 시 댓글 작성자에게 알림 메일을 발송합니다.
     *
     * @param commentId 좋아요를 누를 댓글 ID
     * @param userId    좋아요를 누르는 사용자 ID
     * @return liked(현재 좋아요 여부), count(현재 좋아요 총 개수)
     */
    @Transactional
    public Result toggle(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다. id=" + commentId));

        // Comment 객체를 사용하여 existsByCommentAndUserId 호출
        boolean wasLiked = likeRepository.existsByCommentAndUserId(comment, userId);
        if (wasLiked) {
            // Comment 객체를 사용하여 deleteByCommentAndUserId 호출
            likeRepository.deleteByCommentAndUserId(comment, userId);
            // Comment 엔티티의 likes 컬렉션에서도 제거 (양방향 관계 동기화)
            comment.removeLike(comment.getLikes().stream()
                    .filter(cl -> cl.getUserId().equals(userId))
                    .findFirst().orElse(null));
        } else {
            CommentLike newLike = new CommentLike(comment, userId); // Comment 객체를 사용하여 CommentLike 생성
            likeRepository.save(newLike);
            comment.addLike(newLike); // Comment 엔티티의 likes 컬렉션에 추가 (양방향 관계 동기화)
            notifyAuthor(comment, userId);
        }
        // Comment 객체를 사용하여 countByComment 호출
        long count = likeRepository.countByComment(comment);
        return new Result(!wasLiked, count);
    }

    /**
     * 댓글 작성자에게 좋아요 알림 메일을 발송합니다.
     * 본인 댓글에 좋아요를 누른 경우 또는 작성자 이메일이 없으면 발송하지 않습니다.
     * 메일 발송 실패 시 예외를 던지지 않고 경고 로그만 남깁니다.
     */
    private void notifyAuthor(Comment comment, Long likerUserId) {
        if (comment.getUserId() == null || comment.getUserId().equals(likerUserId)) return;
        try {
            User author = userRepository.findById(comment.getUserId()).orElse(null);
            User liker = userRepository.findById(likerUserId).orElse(null);
            Notice notice = noticeRepository.findById(comment.getNoticeId()).orElse(null);
            if (author == null || author.getEmail() == null || author.getEmail().isBlank()) return;
            String url = frontendUrl + "/notices/" + comment.getNoticeId();
            String excerpt = comment.getContent();
            if (excerpt != null && excerpt.length() > 200) excerpt = excerpt.substring(0, 200) + "…";
            emailService.sendCommentLiked(
                    author.getEmail(),
                    displayName(author),
                    displayName(liker),
                    notice != null ? notice.getTitle() : "(게시글 없음)",
                    excerpt == null ? "" : excerpt,
                    url
            );
        } catch (Exception e) {
            log.warn("[CommentLike] 알림 메일 실패 commentId={}, liker={}: {}",
                    comment.getId(), likerUserId, e.getMessage());
        }
    }

    /**
     * 여러 댓글의 좋아요 수를 한 번에 조회합니다.
     *
     * @param ids 조회할 댓글 ID 목록
     * @return {댓글ID: 좋아요수} 형태의 Map. 좋아요가 없는 댓글은 Map에 포함되지 않습니다.
     */
    public Map<Long, Long> countsByCommentIds(Collection<Long> ids) {
        Map<Long, Long> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) return map;
        for (Object[] row : likeRepository.countGroupByCommentIds(ids)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    /**
     * 특정 사용자가 좋아요를 누른 댓글 ID 목록을 반환합니다.
     *
     * @param userId     조회할 사용자 ID
     * @param commentIds 확인할 댓글 ID 목록
     * @return 사용자가 좋아요를 누른 댓글 ID의 Set
     */
    public Set<Long> likedCommentIdsByUser(Long userId, Collection<Long> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) return Set.of();
        return new HashSet<>(likeRepository.findLikedCommentIds(userId, commentIds));
    }

    private String displayName(User u) {
        if (u == null) return "사용자";
        if (u.getNickname() != null && !u.getNickname().isBlank()) return u.getNickname();
        return u.getUsername() != null ? u.getUsername() : "사용자";
    }

    public record Result(boolean liked, long count) {}
}
