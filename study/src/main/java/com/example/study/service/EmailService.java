package com.example.study.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.mail.from-name}") String fromName
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Async
    public void sendWelcome(String toEmail, String displayName) {
        String subject = "[Study Notice] 회원가입을 환영합니다 🎉";
        String html = """
                <div style="font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#1e293b;">
                  <h2 style="color:#6366f1;">%s님, 환영합니다!</h2>
                  <p>Study Notice 가입이 완료되었어요. 공지사항·책 검색·지도/날씨/미세먼지 기능을 자유롭게 이용해 보세요.</p>
                  <p style="color:#64748b;font-size:13px;">이 메일은 가입 알림용 자동 발송 메일입니다.</p>
                </div>
                """.formatted(displayName != null && !displayName.isBlank() ? displayName : "사용자");
        send(toEmail, subject, html);
    }

    @Async
    public void sendPasswordReset(String toEmail, String displayName, String resetUrl) {
        String subject = "[Study Notice] 비밀번호 재설정 안내";
        String html = """
                <div style="font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#1e293b;">
                  <h2 style="color:#6366f1;">비밀번호 재설정</h2>
                  <p>%s님, 아래 버튼을 눌러 30분 이내에 비밀번호를 재설정해 주세요.</p>
                  <p>
                    <a href="%s" style="display:inline-block;padding:12px 18px;background:linear-gradient(135deg,#a855f7,#6366f1);color:#fff;border-radius:10px;text-decoration:none;font-weight:700;">
                      비밀번호 재설정
                    </a>
                  </p>
                  <p style="color:#64748b;font-size:13px;">버튼이 보이지 않으면 다음 링크를 복사해 주세요:<br>%s</p>
                  <p style="color:#94a3b8;font-size:12px;">본 메일은 자동 발송되었으며, 본인이 요청하지 않았다면 무시해도 됩니다.</p>
                </div>
                """.formatted(
                        displayName != null && !displayName.isBlank() ? displayName : "사용자",
                        resetUrl, resetUrl);
        send(toEmail, subject, html);
    }

    @Async
    public void sendPasswordChanged(String toEmail, String displayName) {
        String subject = "[Study Notice] 비밀번호가 변경되었습니다";
        String html = """
                <div style="font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#1e293b;">
                  <h2 style="color:#6366f1;">비밀번호 변경 안내</h2>
                  <p>%s님, 방금 계정의 비밀번호가 변경되었어요.</p>
                  <p>본인이 변경한 것이 아니라면 즉시 비밀번호 찾기 기능으로 재설정하고 관리자에게 문의해 주세요.</p>
                  <p style="color:#94a3b8;font-size:12px;">본 메일은 자동 발송되었습니다.</p>
                </div>
                """.formatted(displayName != null && !displayName.isBlank() ? displayName : "사용자");
        send(toEmail, subject, html);
    }

    @Async
    public void sendBroadcast(java.util.List<String> recipients, String subject, String html) {
        if (recipients == null || recipients.isEmpty()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(fromAddress);
            helper.setBcc(recipients.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Broadcast email sent to {} recipients - {}", recipients.size(), subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.warn("Failed to send broadcast ({}): {}", subject, e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Mail provider error for broadcast ({}): {}", subject, e.getMessage(), e);
        }
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to {} - {}", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.warn("Failed to send email to {} ({}): {}", to, subject, e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Mail provider error to {} ({}): {}", to, subject, e.getMessage(), e);
        }
    }
}