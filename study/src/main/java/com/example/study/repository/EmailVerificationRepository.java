package com.example.study.repository;

import com.example.study.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    // 이메일로 인증 정보 조회 (인증 코드 확인 시 사용)
    // SQL: SELECT * FROM email_verification WHERE email = ?
    Optional<EmailVerification> findByEmail(String email);

    // 이메일 인증 완료 후 인증 정보 삭제
    // SQL: DELETE FROM email_verification WHERE email = ?
    @Transactional
    void deleteByEmail(String email);
}
