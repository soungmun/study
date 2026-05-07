package com.example.study.controller;

import com.example.study.dto.response.AirQualityResponse;
import com.example.study.service.AirQualityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/air")
public class AirQualityApiController {

    private final AirQualityService service;

    public AirQualityApiController(AirQualityService service) {
        this.service = service;
    }

    @GetMapping
    public AirQualityResponse get(
            @RequestParam double lat,
            @RequestParam double lng) {
        return service.get(lat, lng);
    }
}