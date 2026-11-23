package com.example.study.repository;

import com.example.study.entity.Comment;
import com.example.study.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    // 특정 사용자가 특정 댓글에 좋아요를 눌렀는지 확인
    // SQL: SELECT COUNT(*) > 0 FROM comment_like WHERE comment_id = ? AND user_id = ?
    boolean existsByCommentAndUserId(Comment comment, Long userId);

    // 특정 댓글의 좋아요 수 조회
    // SQL: SELECT COUNT(*) FROM comment_like WHERE comment_id = ?
    long countByComment(Comment comment);

    // 특정 사용자의 특정 댓글 좋아요 삭제 (좋아요 취소)
    // SQL: DELETE FROM comment_like WHERE comment_id = ? AND user_id = ?
    @Transactional
    long deleteByCommentAndUserId(Comment comment, Long userId);

    // 여러 댓글의 좋아요 수를 한 번에 조회 (N+1 방지)
    // SQL: SELECT comment_id, COUNT(*) FROM comment_like WHERE comment_id IN (...) GROUP BY comment_id
    // 반환: Object[]{ comment_id(Long), 좋아요수(Long) } 리스트
    @Query("SELECT cl.comment.id, COUNT(cl) FROM CommentLike cl WHERE cl.comment.id IN :ids GROUP BY cl.comment.id")
    List<Object[]> countGroupByCommentIds(@Param("ids") Collection<Long> ids);

    // 특정 사용자가 좋아요 누른 댓글 ID 목록 조회
    // SQL: SELECT comment_id FROM comment_like WHERE user_id = ? AND comment_id IN (...)
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.userId = :userId AND cl.comment.id IN :ids")
    List<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    // 특정 사용자가 누른 댓글 좋아요 전체 삭제 (회원 탈퇴 시 사용)
    // SQL: DELETE FROM comment_like WHERE user_id = ?
    @Transactional
    void deleteByUserId(Long userId);
}
