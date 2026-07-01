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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 공지사항 게시글의 '좋아요' 기능과 관련된 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 좋아요 토글(추가/취소), 좋아요 누른 사용자 목록 조회, 개수 집계 등을 담당합니다.
 */
@Service
@Transactional(readOnly = true)
public class NoticeLikeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeLikeService.class);

    private final NoticeLikeRepository likeRepository;
    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;

    public NoticeLikeService(NoticeLikeRepository likeRepository,
                             NoticeRepository noticeRepository,
                             UserRepository userRepository) {
        this.likeRepository = likeRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
    }

    /**
     * 공지사항에 대한 좋아요를 토글합니다.
     * 이미 좋아요를 누른 상태라면 취소(삭제)하고, 누르지 않은 상태라면 추가(저장)합니다.
     *
     * @param noticeId 좋아요를 누를 공지사항 ID
     * @param userId 좋아요를 누르는 사용자 ID
     * @return 토글 후의 좋아요 상태(liked)와 총 좋아요 수(count)를 포함한 Result 객체
     */
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
        }
        // Notice 객체를 사용하여 countByNotice 호출
        long count = likeRepository.countByNotice(notice);
        return new Result(!wasLiked, count);
    }


    /**
     * 특정 공지사항에 좋아요를 누른 사용자 목록을 최신순으로 조회합니다.
     *
     * @param noticeId 공지사항 ID
     * @return 좋아요를 누른 사용자들의 프로필 정보 목록
     */
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

    /**
     * 특정 공지사항의 전체 좋아요 개수를 반환합니다.
     *
     * @param noticeId 공지사항 ID
     * @return 좋아요 개수
     */
    public long count(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + noticeId));
        return likeRepository.countByNotice(notice);
    }

    /**
     * 특정 사용자가 해당 공지사항에 좋아요를 눌렀는지 여부를 확인합니다.
     *
     * @param noticeId 공지사항 ID
     * @param userId 확인할 사용자 ID
     * @return 좋아요를 눌렀다면 true, 아니면 false
     */
    public boolean liked(Long noticeId, Long userId) {
        if (userId == null) return false;
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + noticeId));
        return likeRepository.existsByNoticeAndUserId(notice, userId);
    }

    /**
     * 여러 공지사항 ID에 대한 좋아요 개수를 한 번에 맵 형태로 반환합니다. (N+1 문제 방지용)
     *
     * @param ids 좋아요 개수를 조회할 공지사항 ID 목록
     * @return 공지사항 ID를 키로, 좋아요 개수를 값으로 갖는 Map
     */
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
