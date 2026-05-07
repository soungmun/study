package com.example.study.controller;

import com.example.study.dto.request.BroadcastRequest;
import com.example.study.dto.response.MessageResponse;
import com.example.study.entity.User;
import com.example.study.repository.UserRepository;
import com.example.study.service.DailyMailScheduler;
import com.example.study.service.EmailService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminMailController {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final DailyMailScheduler dailyMailScheduler;
    private final String adminUsername;

    public AdminMailController(
            UserRepository userRepository,
            EmailService emailService,
            DailyMailScheduler dailyMailScheduler,
            @Value("${app.admin.username:}") String adminUsername
    ) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.dailyMailScheduler = dailyMailScheduler;
        this.adminUsername = adminUsername;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        boolean admin = isAdmin(session);
        long subscribers = admin ? userRepository.findByNotificationOptInTrue().stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .count() : 0L;
        return ResponseEntity.ok(Map.of("admin", admin, "subscribers", subscribers));
    }

    @PostMapping("/broadcast/daily-now")
    public ResponseEntity<?> triggerDailyNow(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403)
                    .body(MessageResponse.of("관리자만 사용할 수 있는 기능입니다."));
        }
        dailyMailScheduler.sendDailyGreeting();
        return ResponseEntity.ok(Map.of("triggered", true));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcast(
            @Valid @RequestBody BroadcastRequest req,
            HttpSession session
    ) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403)
                    .body(MessageResponse.of("관리자만 사용할 수 있는 기능입니다."));
        }
        List<String> recipients = userRepository.findByNotificationOptInTrue().stream()
                .map(User::getEmail)
                .filter(e -> e != null && !e.isBlank())
                .distinct()
                .toList();
        if (recipients.isEmpty()) {
            return ResponseEntity.status(400)
                    .body(MessageResponse.of("수신 동의한 사용자가 없습니다."));
        }
        String html = """
                <div style="font-family:'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#1e293b;line-height:1.7;">
                  <h2 style="color:#6366f1;">%s</h2>
                  <div>%s</div>
                  <p style="color:#94a3b8;font-size:12px;margin-top:24px;">본 메일은 수신 동의하신 회원에게 발송되었습니다. 수신을 원치 않으시면 사이트 회원정보 수정에서 동의를 해제해 주세요.</p>
                </div>
                """.formatted(escape(req.subject()), req.body().replace("\n", "<br>"));
        emailService.sendBroadcast(recipients, req.subject(), html);
        return ResponseEntity.ok(Map.of(
                "queued", true,
                "recipients", recipients.size()
        ));
    }

    private boolean isAdmin(HttpSession session) {
        if (adminUsername == null || adminUsername.isBlank()) return false;
        Object id = session.getAttribute(AuthController.SESSION_USER_KEY);
        if (!(id instanceof Long userId)) return false;
        return userRepository.findById(userId)
                .map(u -> adminUsername.equals(u.getUsername()))
                .orElse(false);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}