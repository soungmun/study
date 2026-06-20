package com.example.study.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 전역 설정.
 * 점검 모드는 Security 필터 체인(MaintenanceFilter)으로 이전되었으므로
 * 인터셉터 등록이 불필요합니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
