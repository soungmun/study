package com.example.study.config;

import com.example.study.repository.UserRepository;
import com.example.study.service.MaintenanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    public MaintenanceInterceptor(MaintenanceService maintenance,
                                  @Value("${app.admin.username:}") String adminUsername,
                                  UserRepository userRepository) {
        this.maintenance = maintenance;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!maintenance.isEnabled()) return true;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if (isWhitelisted(request.getRequestURI())) return true;
        if (isAdmin()) return true;

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

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
