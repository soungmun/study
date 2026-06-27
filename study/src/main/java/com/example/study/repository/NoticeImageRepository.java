package com.example.study.repository;

import com.example.study.entity.Notice; // Notice 엔티티 import
import com.example.study.entity.NoticeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NoticeImageRepository extends JpaRepository<NoticeImage, Long> {

    // 기존 findByNoticeIdOrderByIdAsc(Long noticeId) 대신 Notice 객체를 받도록 변경
    List<NoticeImage> findByNoticeOrderByIdAsc(Notice notice);

    List<NoticeImage> findByIdInAndUserId(List<Long> ids, Long userId);

    List<NoticeImage> findByIdIn(List<Long> ids);

    // 회원 탈퇴 시 해당 사용자가 업로드한 이미지 조회
    List<NoticeImage> findByUserId(Long userId);

    @Transactional
    // 기존 deleteByNoticeId(Long noticeId) 대신 Notice 객체를 받도록 변경
    void deleteByNotice(Notice notice);
}
