package com.example.study.repository;

import com.example.study.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 카카오 소셜 로그인 시 사용자 조회
    // SQL: SELECT * FROM users WHERE kakao_id = ?
    Optional<User> findByKakaoId(Long kakaoId);

    // 네이버 소셜 로그인 시 사용자 조회
    // SQL: SELECT * FROM users WHERE naver_id = ?
    Optional<User> findByNaverId(String naverId);

    // 구글 소셜 로그인 시 사용자 조회
    // SQL: SELECT * FROM users WHERE google_id = ?
    Optional<User> findByGoogleId(String googleId);

    // 일반 로그인 시 사용자 조회
    // SQL: SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    // 이메일로 사용자 조회 (비밀번호 찾기, 이메일 인증 등)
    // SQL: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // 비밀번호 재설정 토큰으로 사용자 조회
    // SQL: SELECT * FROM users WHERE reset_token = ?
    Optional<User> findByResetToken(String resetToken);

    // 아이디 중복 확인
    // SQL: SELECT COUNT(*) > 0 FROM users WHERE username = ?
    boolean existsByUsername(String username);

    // 닉네임 중복 확인 (본인 제외) - 프로필 수정 시 사용
    // SQL: SELECT COUNT(*) > 0 FROM users WHERE nickname = ? AND id != ?
    boolean existsByNicknameAndIdNot(String nickname, Long id);

    // 닉네임 중복 확인 - 회원가입 시 사용
    // SQL: SELECT COUNT(*) > 0 FROM users WHERE nickname = ?
    boolean existsByNickname(String nickname);

    // 이메일 수신 동의한 사용자 전체 조회 (관리자 공지 발송 시 사용)
    // SQL: SELECT * FROM users WHERE notification_opt_in = true
    List<User> findByNotificationOptInTrue();
}
