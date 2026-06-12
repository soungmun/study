package com.example.study.config;

import com.example.study.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminRoleInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminRoleInitializer.class);

    private final UserRepository userRepository;
    private final String adminUsername;

    public AdminRoleInitializer(UserRepository userRepository,
                                @Value("${app.admin.username:}") String adminUsername) {
        this.userRepository = userRepository;
        this.adminUsername = adminUsername;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        if (adminUsername == null || adminUsername.isBlank()) return;
        userRepository.findByUsername(adminUsername).ifPresent(u -> {
            if (!"ADMIN".equals(u.getRole())) {
                u.setRole("ADMIN");
                userRepository.save(u);
                log.info("[AdminRole] '{}' → ADMIN 설정 완료", adminUsername);
            }
        });
    }
}
