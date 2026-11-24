package com.example.study.repository;

import com.example.study.entity.Notice;
import com.example.study.entity.NoticeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NoticeImageRepository extends JpaRepository<NoticeImage, Long> {

    // 특정 이미지 ID 목록 중 특정 사용자가 업로드한 이미지 조회 (게시글 저장 시 소유권 확인)
    // SQL: SELECT * FROM notice_image WHERE id IN (...) AND user_id = ?
    List<NoticeImage> findByIdInAndUserId(List<Long> ids, Long userId);

    // 특정 사용자가 업로드한 이미지 전체 조회 (회원 탈퇴 시 사용)
    // SQL: SELECT * FROM notice_image WHERE user_id = ?
    List<NoticeImage> findByUserId(Long userId);

    // 특정 공지사항에 연결된 이미지 전체 삭제 (공지 삭제 시 사용)
    // SQL: DELETE FROM notice_image WHERE notice_id = ?
    @Transactional
    void deleteByNotice(Notice notice);
}
