package com.example.study.repository;

import com.example.study.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 특정 사용자가 작성한 공지사항 전체 조회 (회원 탈퇴 시 사용)
    // SQL: SELECT * FROM notice WHERE author_id = ?
    List<Notice> findByAuthorId(Long authorId);

    // 제목에 키워드가 포함된 공지사항 페이징 조회
    // SQL: SELECT * FROM notice WHERE title LIKE '%keyword%' LIMIT ? OFFSET ?
    @Query("SELECT n FROM Notice n WHERE n.title LIKE %:keyword%")
    Page<Notice> findByTitleLike(@Param("keyword") String keyword, Pageable pageable);

    // 내용에 키워드가 포함된 공지사항 페이징 조회
    // SQL: SELECT * FROM notice WHERE content LIKE '%keyword%' LIMIT ? OFFSET ?
    @Query("SELECT n FROM Notice n WHERE n.content LIKE %:keyword%")
    Page<Notice> findByContentLike(@Param("keyword") String keyword, Pageable pageable);
}
