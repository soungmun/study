package com.example.study.controller;

import com.example.study.service.DailyMailScheduler;
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
            @RequestParam(value = "force", defaultValue = "false") boolean force) {

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
}