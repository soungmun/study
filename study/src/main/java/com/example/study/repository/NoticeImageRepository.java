package com.example.study.repository;

import com.example.study.entity.NoticeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NoticeImageRepository extends JpaRepository<NoticeImage, Long> {

    List<NoticeImage> findByNoticeIdOrderByIdAsc(Long noticeId);

    List<NoticeImage> findByIdInAndUserId(List<Long> ids, Long userId);

    @Transactional
    void deleteByNoticeId(Long noticeId);
}
