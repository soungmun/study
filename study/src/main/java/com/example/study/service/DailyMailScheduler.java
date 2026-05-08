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
    private final StockService stockService;

    public DailyMailScheduler(
            UserRepository userRepository,
            EmailService emailService,
            StockService stockService
    ) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.stockService = stockService;
    }

    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Seoul")
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
        String subject = "[Study Notice] " + today + " 오늘도 수고하셨어요!";
        List<StockService.Quote> quotes = stockService.fetchWatchlist();
        String html = buildHtml(today, quotes);
        emailService.sendBroadcast(recipients, subject, html);
        log.info("[DailyMail] 발송 큐 등록 완료 — 수신자 {}명", recipients.size());
    }

    private String buildHtml(String today, List<StockService.Quote> quotes) {
        StringBuilder rows = new StringBuilder();
        for (StockService.Quote q : quotes) {
            rows.append(stockRow(q));
        }
        return """
                <div style="font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#1e293b;line-height:1.7;">
                  <h2 style="color:#6366f1;margin-bottom:8px;">🌆 %s</h2>
                  <p style="font-size:15px;">오늘 하루도 수고하셨어요!</p>
                  <p style="font-size:15px;">편안한 저녁 보내시고 푹 쉬세요 🙂</p>

                  <h3 style="color:#1e293b;margin-top:28px;border-bottom:2px solid #e2e8f0;padding-bottom:6px;">📈 오늘의 주요 지수·종목</h3>
                  <table style="border-collapse:collapse;width:100%%;font-size:14px;margin-top:8px;">
                    <thead>
                      <tr style="background:#f1f5f9;color:#475569;">
                        <th style="padding:8px 10px;text-align:left;border-bottom:1px solid #e2e8f0;">종목</th>
                        <th style="padding:8px 10px;text-align:right;border-bottom:1px solid #e2e8f0;">가격</th>
                        <th style="padding:8px 10px;text-align:right;border-bottom:1px solid #e2e8f0;">변동</th>
                        <th style="padding:8px 10px;text-align:right;border-bottom:1px solid #e2e8f0;">변동률</th>
                      </tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                  <p style="color:#94a3b8;font-size:11px;margin-top:6px;">출처: Yahoo Finance · 발송 시점 직전 시세</p>

                  <p style="color:#94a3b8;font-size:12px;margin-top:24px;">
                    본 메일은 매일 오후 8시에 자동 발송됩니다. 수신을 원치 않으시면
                    사이트 회원정보 수정에서 '공지 메일 수신 동의'를 해제해 주세요.
                  </p>
                </div>
                """.formatted(today, rows.toString());
    }

    private String stockRow(StockService.Quote q) {
        if (q.price() == null) {
            return """
                    <tr>
                      <td style="padding:8px 10px;border-bottom:1px solid #f1f5f9;">%s</td>
                      <td colspan="3" style="padding:8px 10px;text-align:right;color:#94a3b8;border-bottom:1px solid #f1f5f9;">정보 없음</td>
                    </tr>
                    """.formatted(q.name());
        }
        double change = q.change() == null ? 0 : q.change();
        double pct = q.changePercent() == null ? 0 : q.changePercent();
        String color = change > 0 ? "#ef4444" : (change < 0 ? "#2563eb" : "#64748b");
        String sign = change > 0 ? "+" : "";
        return """
                <tr>
                  <td style="padding:8px 10px;border-bottom:1px solid #f1f5f9;">%s</td>
                  <td style="padding:8px 10px;text-align:right;border-bottom:1px solid #f1f5f9;">%s</td>
                  <td style="padding:8px 10px;text-align:right;color:%s;border-bottom:1px solid #f1f5f9;">%s%s</td>
                  <td style="padding:8px 10px;text-align:right;color:%s;border-bottom:1px solid #f1f5f9;">%s%.2f%%</td>
                </tr>
                """.formatted(
                q.name(),
                formatPrice(q.price()),
                color, sign, formatPrice(change),
                color, sign, pct);
    }

    private String formatPrice(double v) {
        // 지수는 소수점 2자리, 종목 가격은 정수 콤마
        if (Math.abs(v) < 100000 && v != Math.floor(v)) {
            return String.format(Locale.US, "%,.2f", v);
        }
        return String.format(Locale.US, "%,.0f", v);
    }
}