package com.example.study.controller;

import com.example.study.dto.response.AirQualityResponse;
import com.example.study.service.AirQualityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 미세먼지(대기질) 정보 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * 실시간 대기질 정보 조회 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/air-quality")
public class AirQualityApiController {

    private final AirQualityService service;

    public AirQualityApiController(AirQualityService service) {
        this.service = service;
    }

    @GetMapping
    public AirQualityResponse get(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng) {
        return service.get(lat, lng);
    }
}