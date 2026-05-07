package com.example.study.controller;

import com.example.study.dto.response.WeatherResponse;
import com.example.study.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
public class WeatherApiController {

    private final WeatherService weatherService;

    public WeatherApiController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public WeatherResponse get(
            @RequestParam double lat,
            @RequestParam double lng) {
        return weatherService.getWeather(lat, lng);
    }
}