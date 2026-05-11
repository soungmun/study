package com.example.study.service;

import com.example.study.entity.User;
import com.example.study.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class DailyMailScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyMailScheduler.class);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime DAILY_TIME = LocalTime.of(20, 0);
    private static final Path STATE_FILE =
            Path.of(System.getProperty("user.home"), ".study-notice-last-daily.txt");

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final StockService stockService;
    private final NewsService newsService;

    public DailyMailScheduler(
            UserRepository userRepository,
            EmailService emailService,
            StockService stockService,
            NewsService newsService
    ) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.stockService = stockService;
        this.newsService = newsService;
    }

    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Seoul")
    public void sendDailyGreeting() {
        trySendForToday("정시");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        if (now.toLocalTime().isBefore(DAILY_TIME)) {
            log.info("[DailyMail] 시작 시점 {} (KST) — 오늘 20:00 이전, 정시 발송 대기", now.toLocalTime());
            return;
        }
        trySendForToday("기동시점-catchup");
    }

    private synchronized void trySendForToday(String trigger) {
        LocalDate today = LocalDate.now(KST);
        LocalDate lastSent = readLastSent();
        if (today.equals(lastSent)) {
            log.info("[DailyMail] [{}] 오늘({})은 이미 발송됨 — 스킵", trigger, today);
            return;
        }
        List<String> recipients = userRepository.findByNotificationOptInTrue().stream()
                .map(User::getEmail)
                .filter(e -> e != null && !e.isBlank())
                .distinct()
                .toList();
        if (recipients.isEmpty()) {
            log.info("[DailyMail] [{}] 수신 동의자 0명 — 발송 스킵", trigger);
            return;
        }
        String todayStr = today.format(DATE_FMT);
        String subject = "[Study Notice] " + todayStr + " 오늘도 수고하셨어요!";
        List<StockService.Quote> quotes = stockService.fetchWatchlist();
        List<NewsService.Headline> news = newsService.fetchTop(8);
        String html = buildHtml(todayStr, quotes, news);
        emailService.sendBroadcast(recipients, subject, html);
        writeLastSent(today);
        log.info("[DailyMail] [{}] 발송 큐 등록 완료 — 수신자 {}명", trigger, recipients.size());
    }

    private LocalDate readLastSent() {
        try {
            if (!Files.exists(STATE_FILE)) return null;
            String s = Files.readString(STATE_FILE).trim();
            return s.isEmpty() ? null : LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("[DailyMail] 상태 파일 읽기 실패: {}", e.getMessage());
            return null;
        }
    }

    private void writeLastSent(LocalDate date) {
        try {
            Files.writeString(STATE_FILE, date.toString());
        } catch (IOException e) {
            log.warn("[DailyMail] 상태 파일 기록 실패: {}", e.getMessage());
        }
    }

    private String buildHtml(String today, List<StockService.Quote> quotes, List<NewsService.Headline> news) {
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

                  %s

                  <p style="color:#94a3b8;font-size:12px;margin-top:24px;">
                    본 메일은 매일 오후 8시에 자동 발송됩니다. 수신을 원치 않으시면
                    사이트 회원정보 수정에서 '공지 메일 수신 동의'를 해제해 주세요.
                  </p>
                </div>
                """.formatted(today, rows.toString(), newsSection(news));
    }

    private String newsSection(List<NewsService.Headline> news) {
        if (news == null || news.isEmpty()) {
            return "";
        }
        StringBuilder items = new StringBuilder();
        for (NewsService.Headline h : news) {
            String src = h.source() == null || h.source().isBlank()
                    ? ""
                    : "<div style=\"color:#64748b;font-size:12px;margin-top:2px;\">" + escape(h.source()) + "</div>";
            items.append("""
                    <li style="padding:10px 0;border-bottom:1px solid #f1f5f9;">
                      <a href="%s" target="_blank" rel="noopener" style="color:#1e293b;text-decoration:none;font-weight:600;">%s</a>
                      %s
                    </li>
                    """.formatted(escape(h.link()), escape(h.title()), src));
        }
        return """
                <h3 style="color:#1e293b;margin-top:28px;border-bottom:2px solid #e2e8f0;padding-bottom:6px;">📰 오늘의 주식 주요 뉴스</h3>
                <ul style="list-style:none;padding:0;margin:8px 0 0;font-size:14px;">%s</ul>
                <p style="color:#94a3b8;font-size:11px;margin-top:6px;">출처: Google News (검색어: 주식·증시·코스피)</p>
                """.formatted(items.toString());
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
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