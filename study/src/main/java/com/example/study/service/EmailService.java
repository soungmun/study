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

    /** 회원가입 인증번호 메일 (동기 — 발송 결과를 사용자가 즉시 알아야 하므로) */
    public void sendVerificationCode(String toEmail, String code, int validMinutes) throws MessagingException, UnsupportedEncodingException {
        String subject = "[Study Notice] 이메일 인증번호: " + code;
        String html = """
                <div style="font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#1e293b;line-height:1.7;">
                  <h2 style="color:#6366f1;">📧 이메일 인증</h2>
                  <p>회원가입을 위한 인증번호입니다. 회원가입 화면에 아래 6자리 숫자를 입력해 주세요.</p>
                  <div style="margin:20px 0;padding:18px;background:#f8fafc;border:2px dashed #6366f1;border-radius:10px;text-align:center;">
                    <div style="font-size:32px;font-weight:800;letter-spacing:8px;color:#6366f1;font-family:monospace;">%s</div>
                  </div>
                  <p style="color:#475569;">인증번호는 <strong>%d분</strong> 동안 유효합니다.</p>
                  <p style="color:#94a3b8;font-size:12px;margin-top:24px;">본인이 요청하지 않았다면 이 메일은 무시해 주세요.</p>
                </div>
                """.formatted(code, validMinutes);
        // 동기 발송 — 예외를 호출자에게 전달
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
        log.info("[Mail] 인증번호 발송 완료 → {}", toEmail);
    }

    @Async
    public void sendNoticeLiked(String toEmail, String authorName, String likerName,
                                 String noticeTitle, String noticeUrl) {
        String subject = "[Study Notice] " + likerName + "님이 회원님의 게시글에 좋아요 ❤️";
        String html = """
                <div style="font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#1e293b;line-height:1.7;">
                  <h2 style="color:#ef4444;">❤️ 새로운 좋아요</h2>
                  <p>%s님, 안녕하세요!</p>
                  <p><strong>%s</strong> 님이 회원님의 게시글을 좋아합니다.</p>
                  <div style="margin:16px 0;padding:14px;background:#f8fafc;border-left:4px solid #ef4444;border-radius:6px;">
                    <div style="font-size:13px;color:#64748b;margin-bottom:4px;">게시글</div>
                    <div style="font-size:16px;font-weight:700;">%s</div>
                  </div>
                  <p><a href="%s" style="display:inline-block;padding:10px 16px;background:#6366f1;color:#fff;border-radius:8px;text-decoration:none;font-weight:600;">게시글 보러가기</a></p>
                  <p style="color:#94a3b8;font-size:12px;margin-top:24px;">본 메일은 회원님의 게시글에 좋아요가 추가될 때 자동 발송됩니다.</p>
                </div>
                """.formatted(escape(authorName), escape(likerName), escape(noticeTitle), noticeUrl);
        send(toEmail, subject, html);
    }

    @Async
    public void sendCommentLiked(String toEmail, String commentAuthor, String likerName,
                                  String noticeTitle, String commentExcerpt, String noticeUrl) {
        String subject = "[Study Notice] " + likerName + "님이 회원님의 댓글에 좋아요 ❤️";
        String html = """
                <div style="font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#1e293b;line-height:1.7;">
                  <h2 style="color:#ef4444;">❤️ 새로운 좋아요 (댓글)</h2>
                  <p>%s님, 안녕하세요!</p>
                  <p><strong>%s</strong> 님이 회원님의 댓글을 좋아합니다.</p>
                  <div style="margin:16px 0;padding:14px;background:#f8fafc;border-left:4px solid #ef4444;border-radius:6px;">
                    <div style="font-size:13px;color:#64748b;margin-bottom:4px;">게시글: %s</div>
                    <div style="font-size:14px;color:#334155;white-space:pre-wrap;">%s</div>
                  </div>
                  <p><a href="%s" style="display:inline-block;padding:10px 16px;background:#6366f1;color:#fff;border-radius:8px;text-decoration:none;font-weight:600;">게시글 보러가기</a></p>
                  <p style="color:#94a3b8;font-size:12px;margin-top:24px;">본 메일은 회원님의 댓글에 좋아요가 추가될 때 자동 발송됩니다.</p>
                </div>
                """.formatted(escape(commentAuthor), escape(likerName), escape(noticeTitle), escape(commentExcerpt), noticeUrl);
        send(toEmail, subject, html);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Async
    public void sendBroadcast(java.util.List<String> recipients, String subject, String html) {
        sendBroadcastSync(recipients, subject, html);
    }

    @Async
    public void sendMaintenanceNotice(java.util.List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) return;
        String subject = "[Study Notice] 🛠️ 서버 점검 안내";
        String html = """
                <div style="font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#1e293b;line-height:1.7;">
                  <h2 style="color:#f59e0b;">🛠️ 서버 점검 중입니다</h2>
                  <p>안녕하세요. 현재 서비스 점검이 진행 중이에요.</p>
                  <p>점검이 끝나는 대로 정상 이용이 가능하도록 신속히 복구할 예정입니다.</p>
                  <p>이용에 불편을 드려 죄송합니다. 양해 부탁드립니다.</p>
                  <p style="color:#94a3b8;font-size:12px;margin-top:24px;">본 메일은 서비스 운영 안내 메일로, 수신 동의 여부와 무관하게 가입 회원에게 발송됩니다.</p>
                </div>
                """;
        sendBroadcastSync(recipients, subject, html);
    }

    /** 동기 단체 메일 — 호출자에게 발송 성공/실패 결과를 즉시 알려야 할 때 사용. */
    public boolean sendBroadcastSync(java.util.List<String> recipients, String subject, String html) {
        if (recipients == null || recipients.isEmpty()) return false;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(fromAddress);
            helper.setBcc(recipients.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("[Mail] 전송 완료 (단체) → 수신자 {}명 | 제목: {}", recipients.size(), subject);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.warn("Failed to send broadcast ({}): {}", subject, e.getMessage(), e);
            throw new RuntimeException("메일 전송 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Mail provider error for broadcast ({}): {}", subject, e.getMessage(), e);
            throw new RuntimeException("메일 제공자 오류: " + e.getMessage(), e);
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
            log.info("[Mail] 전송 완료 → {} | 제목: {}", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.warn("Failed to send email to {} ({}): {}", to, subject, e.getMessage(), e);
        } catch (Exception e) {
            log.warn("Mail provider error to {} ({}): {}", to, subject, e.getMessage(), e);
        }
    }
}