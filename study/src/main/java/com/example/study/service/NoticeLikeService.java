package com.example.study.service;

import com.example.study.entity.NoticeLike;
import com.example.study.repository.NoticeLikeRepository;
import com.example.study.repository.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class NoticeLikeService {

    private final NoticeLikeRepository likeRepository;
    private final NoticeRepository noticeRepository;

    public NoticeLikeService(NoticeLikeRepository likeRepository,
                             NoticeRepository noticeRepository) {
        this.likeRepository = likeRepository;
        this.noticeRepository = noticeRepository;
    }

    /** 좋아요 토글 — 있으면 취소, 없으면 추가. 반환: { liked, count } */
    @Transactional
    public Result toggle(Long noticeId, Long userId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + noticeId);
        }
        boolean wasLiked = likeRepository.existsByNoticeIdAndUserId(noticeId, userId);
        if (wasLiked) {
            likeRepository.deleteByNoticeIdAndUserId(noticeId, userId);
        } else {
            likeRepository.save(new NoticeLike(noticeId, userId));
        }
        long count = likeRepository.countByNoticeId(noticeId);
        return new Result(!wasLiked, count);
    }

    public long count(Long noticeId) {
        return likeRepository.countByNoticeId(noticeId);
    }

    public boolean liked(Long noticeId, Long userId) {
        if (userId == null) return false;
        return likeRepository.existsByNoticeIdAndUserId(noticeId, userId);
    }

    /** 여러 게시글의 좋아요 수를 한 쿼리로 (group-by) */
    public Map<Long, Long> countsByNoticeIds(Collection<Long> ids) {
        Map<Long, Long> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) return map;
        for (Object[] row : likeRepository.countGroupByNoticeIds(ids)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    public record Result(boolean liked, long count) {}
}
