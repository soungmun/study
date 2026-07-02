package com.example.study.controller;

import com.example.study.config.SecurityUser;
import com.example.study.dto.response.MessageResponse;
import com.example.study.entity.User;
import com.example.study.repository.UserRepository;
import com.example.study.service.EmailService;
import com.example.study.service.MaintenanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자용 시스템 점검 상태 관리 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * 점검 모드 켜기/끄기 및 점검 상태 조회 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/admin/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenance;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public MaintenanceController(MaintenanceService maintenance,
                                 UserRepository userRepository,
                                 EmailService emailService) {
        this.maintenance = maintenance;
        this.userRepository = userRepository;
        this.emailService = emailService;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> enable(@AuthenticationPrincipal SecurityUser principal) {
        boolean changed = maintenance.enable(principal.getUser().getUsername());
        if (!changed) {
            return ResponseEntity.ok(Map.of("enabled", true, "alreadyOn", true, "recipientsQueued", 0));
        }
        List<String> recipients = userRepository.findAll().stream()
                .map(User::getEmail).filter(e -> e != null && !e.isBlank()).distinct().toList();
        emailService.sendMaintenanceNotice(recipients);
        return ResponseEntity.ok(Map.of("enabled", true, "alreadyOn", false, "recipientsQueued", recipients.size()));
    }

    @PostMapping("/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disable() {
        boolean changed = maintenance.disable();
        if (!changed) {
            return ResponseEntity.ok(Map.of("enabled", false, "alreadyOff", true, "recipientsQueued", 0));
        }
        List<String> recipients = userRepository.findAll().stream()
                .map(User::getEmail).filter(e -> e != null && !e.isBlank()).distinct().toList();
        emailService.sendMaintenanceEndNotice(recipients);
        return ResponseEntity.ok(Map.of("enabled", false, "alreadyOff", false, "recipientsQueued", recipients.size()));
    }
}
