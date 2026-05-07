package com.example.study.dto;

public record WeatherResponse(
        double latitude,
        double longitude,
        Double temperature,
        Double windspeed,
        Integer winddirection,
        Integer weathercode,
        String description,
        String icon,
        String time
) {}
