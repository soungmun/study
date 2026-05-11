package com.example.study.service;

import com.example.study.entity.Comment;
import com.example.study.entity.CommentLike;
import com.example.study.entity.Notice;
import com.example.study.entity.User;
import com.example.study.repository.CommentLikeRepository;
import com.example.study.repository.CommentRepository;
import com.example.study.repository.NoticeRepository;
import com.example.study.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CommentLikeService {

    private static final Logger log = LoggerFactory.getLogger(CommentLikeService.class);

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

    @Transactional
    public Result toggle(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다. id=" + commentId));

        boolean wasLiked = likeRepository.existsByCommentIdAndUserId(commentId, userId);
        if (wasLiked) {
            likeRepository.deleteByCommentIdAndUserId(commentId, userId);
        } else {
            likeRepository.save(new CommentLike(commentId, userId));
            notifyAuthor(comment, userId);
        }
        long count = likeRepository.countByCommentId(commentId);
        return new Result(!wasLiked, count);
    }

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

    public Map<Long, Long> countsByCommentIds(Collection<Long> ids) {
        Map<Long, Long> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) return map;
        for (Object[] row : likeRepository.countGroupByCommentIds(ids)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

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
