package com.example.study.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/notice-images}")
    private String uploadDir;

    /**
     * /uploads/notice-images/** 요청을 로컬 파일 시스템의 업로드 디렉토리로 매핑.
     * 예: GET /uploads/notice-images/uuid.jpg → <uploadDir>/uuid.jpg
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // uploadDir이 "uploads/notice-images" 라면 상위 디렉토리인 "uploads"를 루트로 매핑
        String absoluteUploadRoot = Paths.get(uploadDir).toAbsolutePath().getParent().toString()
                .replace("\\", "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadRoot + "/");
    }
}
