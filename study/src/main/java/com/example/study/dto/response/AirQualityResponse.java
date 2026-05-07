package com.example.study.dto.response;

public record AirQualityResponse(
        double latitude,
        double longitude,
        Double pm10,
        Double pm25,
        Integer europeanAqi,
        String pm10Grade,
        String pm25Grade,
        String pm10Color,
        String pm25Color,
        String time
) {}