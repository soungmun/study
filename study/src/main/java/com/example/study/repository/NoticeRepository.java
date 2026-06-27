package com.example.study.repository;

import com.example.study.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 회원 탈퇴 시 해당 사용자가 작성한 게시글 조회
    List<Notice> findByAuthorId(Long authorId);

    // findByTitleContaining 대신 @Query를 사용하여 명시적으로 LIKE 쿼리 정의
    @Query("SELECT n FROM Notice n WHERE n.title LIKE %:keyword%")
    Page<Notice> findByTitleLike(@Param("keyword") String keyword, Pageable pageable);

    // findByContentContaining 대신 @Query를 사용하여 명시적으로 LIKE 쿼리 정의
    @Query("SELECT n FROM Notice n WHERE n.content LIKE %:keyword%")
    Page<Notice> findByContentLike(@Param("keyword") String keyword, Pageable pageable);
}