package com.example.study.controller;

import com.example.study.dto.response.MessageResponse;
import com.example.study.entity.User;
import com.example.study.repository.UserRepository;
import com.example.study.service.EmailService;
import com.example.study.service.MaintenanceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenance;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final String adminUsername;

    public MaintenanceController(
            MaintenanceService maintenance,
            UserRepository userRepository,
            EmailService emailService,
            @Value("${app.admin.username:}") String adminUsername
    ) {
        this.maintenance = maintenance;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.adminUsername = adminUsername;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        Map<String, Object> body = new HashMap<>();
        body.put("enabled", maintenance.isEnabled());
        body.put("lastEnabledBy", maintenance.getLastEnabledBy());
        body.put("lastEnabledAt", maintenance.getLastEnabledAt());
        body.put("lastDisabledAt", maintenance.getLastDisabledAt());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/enable")
    public ResponseEntity<?> enable(HttpSession session) {
        User admin = requireAdmin(session);
        if (admin == null) {
            return ResponseEntity.status(403)
                    .body(MessageResponse.of("관리자만 사용할 수 있는 기능입니다."));
        }
        boolean changed = maintenance.enable(admin.getUsername());
        if (!changed) {
            return ResponseEntity.ok(Map.of(
                    "enabled", true,
                    "alreadyOn", true,
                    "recipientsQueued", 0
            ));
        }
        List<String> recipients = userRepository.findAll().stream()
                .map(User::getEmail)
                .filter(e -> e != null && !e.isBlank())
                .distinct()
                .toList();
        emailService.sendMaintenanceNotice(recipients);
        return ResponseEntity.ok(Map.of(
                "enabled", true,
                "alreadyOn", false,
                "recipientsQueued", recipients.size()
        ));
    }

    @PostMapping("/disable")
    public ResponseEntity<?> disable(HttpSession session) {
        User admin = requireAdmin(session);
        if (admin == null) {
            return ResponseEntity.status(403)
                    .body(MessageResponse.of("관리자만 사용할 수 있는 기능입니다."));
        }
        maintenance.disable();
        return ResponseEntity.ok(Map.of("enabled", false));
    }

    private User requireAdmin(HttpSession session) {
        if (adminUsername == null || adminUsername.isBlank()) return null;
        Object id = session.getAttribute(AuthController.SESSION_USER_KEY);
        if (!(id instanceof Long userId)) return null;
        return userRepository.findById(userId)
                .filter(u -> adminUsername.equals(u.getUsername()))
                .orElse(null);
    }
}
