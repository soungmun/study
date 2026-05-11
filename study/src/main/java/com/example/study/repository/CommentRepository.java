package com.example.study.repository;

import com.example.study.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByNoticeIdOrderByCreatedAtAsc(Long noticeId);
    long countByNoticeId(Long noticeId);
    void deleteByNoticeId(Long noticeId);
}
