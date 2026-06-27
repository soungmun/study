package com.example.study.repository;

import com.example.study.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByNoticeIdOrderByCreatedAtAsc(Long noticeId);

    // 회원 탈퇴 시 해당 사용자가 작성한 댓글 조회
    List<Comment> findByUserId(Long userId);
    long countByNoticeId(Long noticeId);
    void deleteByNoticeId(Long noticeId);

    @Query("SELECT c.noticeId, COUNT(c) FROM Comment c WHERE c.noticeId IN :ids GROUP BY c.noticeId")
    List<Object[]> countGroupByNoticeIds(@Param("ids") Collection<Long> ids);
}
