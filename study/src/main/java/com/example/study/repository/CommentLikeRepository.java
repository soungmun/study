package com.example.study.repository;

import com.example.study.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    long countByCommentId(Long commentId);

    @Transactional
    long deleteByCommentIdAndUserId(Long commentId, Long userId);

    @Query("SELECT cl.commentId, COUNT(cl) FROM CommentLike cl WHERE cl.commentId IN :ids GROUP BY cl.commentId")
    List<Object[]> countGroupByCommentIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT cl.commentId FROM CommentLike cl WHERE cl.userId = :userId AND cl.commentId IN :ids")
    List<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}