package com.example.study.service;

import com.example.study.entity.User;
import com.example.study.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class DailyMailScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyMailScheduler.class);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN);

    private final UserRepository userRepository;
    private final EmailService emailService;

    public DailyMailScheduler(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendDailyGreeting() {
        List<String> recipients = userRepository.findByNotificationOptInTrue().stream()
                .map(User::getEmail)
                .filter(e -> e != null && !e.isBlank())
                .distinct()
                .toList();
        if (recipients.isEmpty()) {
            log.info("[DailyMail] 수신 동의자 0명 — 발송 스킵");
            return;
        }
        String today = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DATE_FMT);
        String subject = "[Study Notice] " + today + " 오늘 하루도 좋은 하루 되세요!";
        String html = """
                <div style="font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#1e293b;line-height:1.7;">
                  <h2 style="color:#6366f1;margin-bottom:8px;">☀️ %s</h2>
                  <p style="font-size:15px;">안녕하세요!</p>
                  <p style="font-size:15px;">오늘 하루도 좋은 하루 보내세요. 활기차고 즐거운 하루가 되시길 바랍니다 🙂</p>
                  <p style="color:#94a3b8;font-size:12px;margin-top:24px;">
                    본 메일은 매일 오전 9시에 자동 발송됩니다. 수신을 원치 않으시면
                    사이트 회원정보 수정에서 '공지 메일 수신 동의'를 해제해 주세요.
                  </p>
                </div>
                """.formatted(today);
        emailService.sendBroadcast(recipients, subject, html);
        log.info("[DailyMail] 발송 큐 등록 완료 — 수신자 {}명", recipients.size());
    }
}