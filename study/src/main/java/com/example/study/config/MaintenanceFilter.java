package com.example.study.config;

import com.example.study.service.MaintenanceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 점검 모드 필터 — Security 필터 체인 안에서 동작하므로
 * SecurityContext 가 이미 복원된 시점에 실행된다.
 */
public class MaintenanceFilter extends OncePerRequestFilter {

    private static final List<String> WHITELIST_PREFIXES = List.of(
            "/api/admin/",
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/me",
            "/api/auth/kakao/",
            "/api/auth/naver/",
            "/api/auth/google/"
    );

    private final MaintenanceService maintenanceService;

    public MaintenanceFilter(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!maintenanceService.isEnabled()
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || isWhitelisted(request.getRequestURI())
                || isAdmin()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"maintenance\":true,\"message\":\"서버 점검 중입니다. 잠시 후 다시 시도해 주세요.\"}"
        );
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
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
