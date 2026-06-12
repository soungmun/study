package com.example.study.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm
                .maximumSessions(1).expiredUrl("/")
            )
            .securityContext(sc -> sc
                .securityContextRepository(new HttpSessionSecurityContextRepository())
            )
            .authorizeHttpRequests(auth -> auth
                // 공개 API
                .requestMatchers(HttpMethod.GET,
                    "/api/notices", "/api/notices/**",
                    "/api/books", "/api/weather/**", "/api/air-quality/**",
                    "/api/places/**", "/api/stock/**", "/api/news/**",
                    "/uploads/**",
                    "/swagger-ui/**", "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(
                    "/api/auth/**",
                    "/api/email-verification/**"
                ).permitAll()
                // 관리자 전용
                .requestMatchers("/api/admin/**", "/api/users").hasRole("ADMIN")
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204))
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            CustomUserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:5175",
            frontendUrl
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        source.registerCorsConfiguration("/uploads/**", cfg);
        return source;
    }
}
