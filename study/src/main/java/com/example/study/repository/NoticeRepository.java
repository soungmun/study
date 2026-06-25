package com.example.study.repository;

import com.example.study.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // findByTitleContaining 대신 @Query를 사용하여 명시적으로 LIKE 쿼리 정의
    @Query("SELECT n FROM Notice n WHERE n.title LIKE %:keyword%")
    Page<Notice> findByTitleLike(@Param("keyword") String keyword, Pageable pageable);

    // findByContentContaining 대신 @Query를 사용하여 명시적으로 LIKE 쿼리 정의
    @Query("SELECT n FROM Notice n WHERE n.content LIKE %:keyword%")
    Page<Notice> findByContentLike(@Param("keyword") String keyword, Pageable pageable);
}