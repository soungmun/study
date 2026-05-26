package com.example.study.config;

import com.example.study.controller.AuthController;
import com.example.study.repository.UserRepository;
import com.example.study.service.MaintenanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class MaintenanceInterceptor implements HandlerInterceptor {

    private static final List<String> WHITELIST_PREFIXES = List.of(
            "/api/admin/",
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/me",
            "/api/auth/kakao/",
            "/api/auth/naver/",
            "/api/auth/google/"
    );

    private final MaintenanceService maintenance;
    private final UserRepository userRepository;
    private final String adminUsername;

    public MaintenanceInterceptor(
            MaintenanceService maintenance,
            UserRepository userRepository,
            @Value("${app.admin.username:}") String adminUsername
    ) {
        this.maintenance = maintenance;
        this.userRepository = userRepository;
        this.adminUsername = adminUsername;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!maintenance.isEnabled()) return true;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        if (isWhitelisted(path)) return true;
        if (isAdmin(request)) return true;

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"maintenance\":true,\"message\":\"서버 점검 중입니다. 잠시 후 다시 시도해 주세요.\"}"
        );
        return false;
    }

    private boolean isWhitelisted(String path) {
        if (path == null) return false;
        for (String prefix : WHITELIST_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix)) return true;
        }
        return false;
    }

    private boolean isAdmin(HttpServletRequest request) {
        if (adminUsername == null || adminUsername.isBlank()) return false;
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        Object id = session.getAttribute(AuthController.SESSION_USER_KEY);
        if (!(id instanceof Long userId)) return false;
        return userRepository.findById(userId)
                .map(u -> adminUsername.equals(u.getUsername()))
                .orElse(false);
    }
}
