package com.example.study.repository;

import com.example.study.entity.Notice;
import com.example.study.entity.NoticeLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface NoticeLikeRepository extends JpaRepository<NoticeLike, Long> {

    // 특정 사용자가 특정 공지사항에 좋아요를 눌렀는지 확인
    // SQL: SELECT COUNT(*) > 0 FROM notice_like WHERE notice_id = ? AND user_id = ?
    boolean existsByNoticeAndUserId(Notice notice, Long userId);

    // 특정 공지사항의 좋아요 수 조회
    // SQL: SELECT COUNT(*) FROM notice_like WHERE notice_id = ?
    long countByNotice(Notice notice);

    // 특정 사용자의 특정 공지사항 좋아요 삭제 (좋아요 취소)
    // SQL: DELETE FROM notice_like WHERE notice_id = ? AND user_id = ?
    @Transactional
    long deleteByNoticeAndUserId(Notice notice, Long userId);

    // 여러 공지사항의 좋아요 수를 한 번에 조회 (N+1 방지)
    // SQL: SELECT notice_id, COUNT(*) FROM notice_like WHERE notice_id IN (...) GROUP BY notice_id
    // 반환: Object[]{ notice_id(Long), 좋아요수(Long) } 리스트
    @Query("SELECT l.notice.id, COUNT(l) FROM NoticeLike l WHERE l.notice.id IN :ids GROUP BY l.notice.id")
    List<Object[]> countGroupByNoticeIds(@Param("ids") Collection<Long> ids);

    // 특정 공지사항의 좋아요 목록 최신순 조회
    // SQL: SELECT * FROM notice_like WHERE notice_id = ? ORDER BY created_at DESC
    List<NoticeLike> findByNoticeOrderByCreatedAtDesc(Notice notice);

    // 특정 사용자가 누른 공지사항 좋아요 전체 삭제 (회원 탈퇴 시 사용)
    // SQL: DELETE FROM notice_like WHERE user_id = ?
    @Transactional
    void deleteByUserId(Long userId);
}
