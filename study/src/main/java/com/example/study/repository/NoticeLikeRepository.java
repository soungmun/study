package com.example.study.repository;

import com.example.study.entity.Notice; // Notice 엔티티 import
import com.example.study.entity.NoticeLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface NoticeLikeRepository extends JpaRepository<NoticeLike, Long> {
    // 기존 existsByNoticeIdAndUserId 대신 Notice 객체를 받도록 변경
    boolean existsByNoticeAndUserId(Notice notice, Long userId);

    // 기존 countByNoticeId 대신 Notice 객체를 받도록 변경
    long countByNotice(Notice notice);

    @Transactional
    // 기존 deleteByNoticeIdAndUserId 대신 Notice 객체를 받도록 변경
    long deleteByNoticeAndUserId(Notice notice, Long userId);

    // JPQL 쿼리도 noticeId 대신 notice.id로 변경
    @Query("SELECT l.notice.id, COUNT(l) FROM NoticeLike l WHERE l.notice.id IN :ids GROUP BY l.notice.id")
    List<Object[]> countGroupByNoticeIds(@Param("ids") Collection<Long> ids);

    // 기존 findByNoticeIdOrderByCreatedAtDesc 대신 Notice 객체를 받도록 변경
    List<NoticeLike> findByNoticeOrderByCreatedAtDesc(Notice notice);

    // 회원 탈퇴 시 해당 사용자가 누른 게시글 좋아요 전체 삭제
    @Transactional
    void deleteByUserId(Long userId);
}
