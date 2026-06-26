package com.example.study.service;

import com.example.study.dto.response.LikedUserResponse;
import com.example.study.entity.Notice;
import com.example.study.entity.NoticeLike;
import com.example.study.entity.User;
import com.example.study.repository.NoticeLikeRepository;
import com.example.study.repository.NoticeRepository;
import com.example.study.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class NoticeLikeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeLikeService.class);

    private final NoticeLikeRepository likeRepository;
    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final String frontendUrl;

    public NoticeLikeService(NoticeLikeRepository likeRepository,
                             NoticeRepository noticeRepository,
                             UserRepository userRepository,
                             EmailService emailService,
                             @Value("${app.frontend.url}") String frontendUrl) {
        this.likeRepository = likeRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public Result toggle(Long noticeId, Long userId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + noticeId));

        // Notice 객체를 사용하여 existsByNoticeAndUserId 호출
        boolean wasLiked = likeRepository.existsByNoticeAndUserId(notice, userId);
        if (wasLiked) {
            // Notice 객체를 사용하여 deleteByNoticeAndUserId 호출
            likeRepository.deleteByNoticeAndUserId(notice, userId);
            // Notice 엔티티의 likes 컬렉션에서도 제거 (양방향 관계 동기화)
            notice.removeLike(notice.getLikes().stream()
                    .filter(nl -> nl.getUserId().equals(userId))
                    .findFirst().orElse(null));
        } else {
            NoticeLike newLike = new NoticeLike(notice, userId); // Notice 객체를 사용하여 NoticeLike 생성
            likeRepository.save(newLike);
            notice.addLike(newLike); // Notice 엔티티의 likes 컬렉션에 추가 (양방향 관계 동기화)
            notifyAuthor(notice, userId);
        }
        // Notice 객체를 사용하여 countByNotice 호출
        long count = likeRepository.countByNotice(notice);
        return new Result(!wasLiked, count);
    }

    /** 좋아요가 새로 추가됐을 때 작성자에게 메일 (작성자 본인이 누른 경우는 제외) */
    private void notifyAuthor(Notice notice, Long likerUserId) {
        if (notice.getAuthorId() == null || notice.getAuthorId().equals(likerUserId)) {
            return;  // 작성자 정보 없거나 자기 자신
        }
        try {
            User author = userRepository.findById(notice.getAuthorId()).orElse(null);
            User liker = userRepository.findById(likerUserId).orElse(null);
            if (author == null || author.getEmail() == null || author.getEmail().isBlank()) return;
            String url = frontendUrl + "/notices/" + notice.getId();
            emailService.sendNoticeLiked(
                    author.getEmail(),
                    displayName(author),
                    displayName(liker),
                    notice.getTitle(),
                    url
            );
        } catch (Exception e) {
            log.warn("[NoticeLike] 알림 메일 실패 noticeId={}, liker={}: {}", notice.getId(), likerUserId, e.getMessage());
        }
    }

    public List<LikedUserResponse> listLikers(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + noticeId));

        // Notice 엔티티의 likes 컬렉션을 직접 사용
        List<NoticeLike> likes = notice.getLikes().stream()
                .sorted((l1, l2) -> l2.getCreatedAt().compareTo(l1.getCreatedAt())) // 최신순 정렬
                .collect(Collectors.toList());


        if (likes.isEmpty()) return List.of();

        Set<Long> userIds = likes.stream().map(NoticeLike::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return likes.stream()
                .map(l -> {
                    User u = userMap.get(l.getUserId());
                    return new LikedUserResponse(
                            l.getUserId(),
                            u != null ? displayName(u) : "(탈퇴 회원)",
                            u != null ? u.getProfileImage() : null,
                            l.getCreatedAt()
                    );
                })
                .toList();
    }

    public long count(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + noticeId));
        return likeRepository.countByNotice(notice);
    }

    public boolean liked(Long noticeId, Long userId) {
        if (userId == null) return false;
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + noticeId));
        return likeRepository.existsByNoticeAndUserId(notice, userId);
    }

    public Map<Long, Long> countsByNoticeIds(Collection<Long> ids) {
        Map<Long, Long> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) return map;
        for (Object[] row : likeRepository.countGroupByNoticeIds(ids)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private String displayName(User u) {
        if (u == null) return "사용자";
        if (u.getNickname() != null && !u.getNickname().isBlank()) return u.getNickname();
        return u.getUsername() != null ? u.getUsername() : "사용자";
    }

    public record Result(boolean liked, long count) {}
}
