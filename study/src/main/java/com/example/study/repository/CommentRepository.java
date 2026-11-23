package com.example.study.repository;

import com.example.study.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 공지사항의 댓글 목록 오래된 순 조회
    // SQL: SELECT * FROM comment WHERE notice_id = ? ORDER BY created_at ASC
    List<Comment> findByNoticeIdOrderByCreatedAtAsc(Long noticeId);

    // 특정 사용자가 작성한 댓글 전체 조회 (회원 탈퇴 시 사용)
    // SQL: SELECT * FROM comment WHERE user_id = ?
    List<Comment> findByUserId(Long userId);

    // 특정 공지사항의 댓글 수 조회
    // SQL: SELECT COUNT(*) FROM comment WHERE notice_id = ?
    long countByNoticeId(Long noticeId);

    // 특정 공지사항의 댓글 전체 삭제 (공지 삭제 시 사용)
    // SQL: DELETE FROM comment WHERE notice_id = ?
    void deleteByNoticeId(Long noticeId);

    // 여러 공지사항의 댓글 수를 한 번에 조회 (N+1 방지)
    // SQL: SELECT notice_id, COUNT(*) FROM comment WHERE notice_id IN (...) GROUP BY notice_id
    // 반환: Object[]{ notice_id(Long), 댓글수(Long) } 리스트
    @Query("SELECT c.noticeId, COUNT(c) FROM Comment c WHERE c.noticeId IN :ids GROUP BY c.noticeId")
    List<Object[]> countGroupByNoticeIds(@Param("ids") Collection<Long> ids);
}
