package com.example.study.repository;

import com.example.study.entity.Comment; // Comment 엔티티 import
import com.example.study.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    // 기존 existsByCommentIdAndUserId 대신 Comment 객체를 받도록 변경
    boolean existsByCommentAndUserId(Comment comment, Long userId);

    // 기존 countByCommentId 대신 Comment 객체를 받도록 변경
    long countByComment(Comment comment);

    @Transactional
    // 기존 deleteByCommentIdAndUserId 대신 Comment 객체를 받도록 변경
    long deleteByCommentAndUserId(Comment comment, Long userId);

    // JPQL 쿼리도 commentId 대신 comment.id로 변경
    @Query("SELECT cl.comment.id, COUNT(cl) FROM CommentLike cl WHERE cl.comment.id IN :ids GROUP BY cl.comment.id")
    List<Object[]> countGroupByCommentIds(@Param("ids") Collection<Long> ids);

    // JPQL 쿼리도 commentId 대신 comment.id로 변경
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.userId = :userId AND cl.comment.id IN :ids")
    List<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}
