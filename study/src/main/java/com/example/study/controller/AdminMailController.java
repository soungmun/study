package com.example.study.controller;

import com.example.study.dto.response.MessageResponse;
import com.example.study.repository.UserRepository;
import com.example.study.service.DailyMailScheduler;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminMailController {

    private final UserRepository userRepository;
    private final DailyMailScheduler dailyMailScheduler;
    private final String adminUsername;

    public AdminMailController(
            UserRepository userRepository,
            DailyMailScheduler dailyMailScheduler,
            @Value("${app.admin.username:}") String adminUsername
    ) {
        this.userRepository = userRepository;
        this.dailyMailScheduler = dailyMailScheduler;
        this.adminUsername = adminUsername;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        return ResponseEntity.ok(Map.of("admin", isAdmin(session)));
    }

    @PostMapping("/broadcast/daily-now")
    public ResponseEntity<?> triggerDailyNow(
            @org.springframework.web.bind.annotation.RequestParam(value = "force", defaultValue = "false") boolean force,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
            HttpSession session
    ) {
        if (!isAdmin(session) && !isValidAdminToken(adminToken)) {
            return ResponseEntity.status(403)
                    .body(MessageResponse.of("관리자만 사용할 수 있는 기능입니다."));
        }
        if (force) {
            DailyMailScheduler.DailyMailResult result = dailyMailScheduler.forceSendNow();
            return ResponseEntity.ok(Map.of(
                    "triggered", true,
                    "sent", result.sent(),
                    "recipients", result.recipientCount(),
                    "message", result.message()
            ));
        }
        dailyMailScheduler.sendDailyGreeting();
        return ResponseEntity.ok(Map.of("triggered", true));
    }

    @Value("${app.admin.token:}")
    private String adminToken;

    private boolean isValidAdminToken(String token) {
        return token != null && !token.isBlank() && adminToken != null && !adminToken.isBlank() && adminToken.equals(token);
    }

    private boolean isAdmin(HttpSession session) {
        if (adminUsername == null || adminUsername.isBlank()) return false;
        Object id = session.getAttribute(AuthController.SESSION_USER_KEY);
        if (!(id instanceof Long userId)) return false;
        return userRepository.findById(userId)
                .map(u -> adminUsername.equals(u.getUsername()))
                .orElse(false);
    }
}