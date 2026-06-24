package com.example.study.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class RealEmailSendTest {

    @Autowired
    private EmailService emailService;

    @Test
    void testRealEmailSend() {
        System.out.println("========== 실제 테스트 메일 전송 시작 ==========");
        String toEmail = "smjo6343@gmail.com";
        String subject = "[Study] AI 에이전트가 보내는 실시간 테스트 메일 🚀";
        String html = """
                <div style="font-family: sans-serif; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;">
                    <h2 style="color: #4f46e5;">안녕하세요! Study Notice 입니다. 🚀</h2>
                    <p>사용자님의 요청에 따라 테스트 메일을 실시간으로 발송합니다.</p>
                    <p>본 메일은 스프링 부트 애플리케이션의 <strong>EmailService</strong>를 통해 실제 SMTP 서버를 거쳐 발송되었습니다.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
                    <p style="font-size: 12px; color: #64748b;">발송 일시: 2026년 6월 24일</p>
                </div>
                """;

        boolean result = emailService.sendBroadcastSync(List.of(toEmail), subject, html);
        System.out.println("전송 결과: " + (result ? "성공" : "실패"));
        System.out.println("========== 실제 테스트 메일 전송 종료 ==========");
    }
}
