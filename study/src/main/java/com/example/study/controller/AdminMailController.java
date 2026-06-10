package com.example.study.controller;

import com.example.study.dto.response.MessageResponse;
import com.example.study.service.DailyMailScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminMailController {

    private final DailyMailScheduler dailyMailScheduler;

    @Value("${app.admin.token:}")
    private String adminToken;

    public AdminMailController(DailyMailScheduler dailyMailScheduler) {
        this.dailyMailScheduler = dailyMailScheduler;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(Map.of("admin", isAdmin));
    }

    @PostMapping("/broadcast/daily-now")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> triggerDailyNow(
            @RequestParam(value = "force", defaultValue = "false") boolean force,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!isValidToken(token)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.status(403).body(MessageResponse.of("관리자만 사용할 수 있는 기능입니다."));
            }
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

    private boolean isValidToken(String token) {
        return token != null && !token.isBlank()
                && adminToken != null && !adminToken.isBlank()
                && adminToken.equals(token);
    }
}
