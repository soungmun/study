package com.example.study.repository;

import com.example.study.entity.NoticeLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface NoticeLikeRepository extends JpaRepository<NoticeLike, Long> {
    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);
    long countByNoticeId(Long noticeId);

    @Transactional
    long deleteByNoticeIdAndUserId(Long noticeId, Long userId);

    @Query("SELECT l.noticeId, COUNT(l) FROM NoticeLike l WHERE l.noticeId IN :ids GROUP BY l.noticeId")
    List<Object[]> countGroupByNoticeIds(@Param("ids") Collection<Long> ids);

    List<NoticeLike> findByNoticeIdOrderByCreatedAtDesc(Long noticeId);
}
