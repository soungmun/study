package com.example.study.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private EmailService emailService;

    @Mock
    private JavaMailSender mailSender; // JavaMailSender를 가짜 객체로 주입합니다.

    @Mock
    private MimeMessage mimeMessage;   // 가짜 메일 메시지 객체

    private final String fromAddress = "sender@study.com";
    private final String fromName = "Study Notice Admin";

    @BeforeEach
    void setUp() {
        // EmailService 생성자 주입 및 가짜 환경 설정 값 주입
        emailService = new EmailService(mailSender, fromAddress, fromName);
    }

    @Test
    @DisplayName("회원가입 완료 메일 발송 테스트")
    void sendWelcomeTest() {
        // given (준비)
        String toEmail = "user@test.com";
        String displayName = "김철수";

        // MimeMessage 생성이 호출되면 Mock MimeMessage를 반환하도록 설정
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when (실행)
        emailService.sendWelcome(toEmail, displayName);

        // then (검증)
        // 1. mailSender.createMimeMessage()가 1번 호출되었는지 검증
        verify(mailSender, times(1)).createMimeMessage();

        // 2. mailSender.send(MimeMessage)가 정상적으로 호출되었는지 검증
        // ArgumentCaptor를 사용하여 send() 메서드에 전달된 MimeMessage 인자를 가로챕니다.
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        MimeMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage).isNotNull();
        assertThat(capturedMessage).isEqualTo(mimeMessage);
    }

    @Test
    @DisplayName("이메일 인증번호 발송 테스트 (동기식)")
    void sendVerificationCodeTest() throws Exception {
        // given (준비)
        String toEmail = "smjo15@naver.com";
        String code = "123456";
        int validMinutes = 5;

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when (실행)
        emailService.sendVerificationCode(toEmail, code, validMinutes);

        // then (검증)
        // 1. createMimeMessage 호출 검증
        verify(mailSender, times(1)).createMimeMessage();

        // 2. send(message)가 정확히 1번 수행되었는지 검증
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        MimeMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage).isEqualTo(mimeMessage);
    }

    @Test
    @DisplayName("단체 안내메일 동기 발송 테스트")
    void sendBroadcastSyncTest() {
        // given (준비)
        List<String> recipients = List.of("user1@test.com", "user2@test.com", "user3@test.com");
        String subject = "[테스트] 단체 공지사항";
        String html = "<h1>안녕하세요 공지 드립니다.</h1>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when (실행)
        boolean result = emailService.sendBroadcastSync(recipients, subject, html);

        // then (검증)
        assertThat(result).isTrue(); // 정상 발송 시 true 반환 검증
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
