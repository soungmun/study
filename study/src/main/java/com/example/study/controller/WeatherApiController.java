package com.example.study.controller;

import com.example.study.dto.response.WeatherResponse;
import com.example.study.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 날씨 정보 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * 현재 위치 기반 단기 예보 및 기상 정보를 제공하는 API를 포함합니다.
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherApiController {

    private final WeatherService weatherService;

    public WeatherApiController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public WeatherResponse get(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng) {
        return weatherService.getWeather(lat, lng);
    }
}