package com.example.study.service;

import com.example.study.dto.AirQualityResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
public class AirQualityService {

    private static final String AIR_URL = "https://air-quality-api.open-meteo.com/v1/air-quality";

    private final RestClient restClient = RestClient.create();

    public AirQualityResponse get(double lat, double lng) {
        URI uri = UriComponentsBuilder.fromUriString(AIR_URL)
                .queryParam("latitude", lat)
                .queryParam("longitude", lng)
                .queryParam("current", "pm10,pm2_5,european_aqi")
                .queryParam("timezone", "Asia/Seoul")
                .build()
                .toUri();

        OpenMeteoAir body = restClient.get()
                .uri(uri)
                .retrieve()
                .body(OpenMeteoAir.class);

        Current cur = body != null ? body.current() : null;
        Double pm10 = cur != null ? cur.pm10() : null;
        Double pm25 = cur != null ? cur.pm2_5() : null;
        Integer aqi = cur != null ? cur.european_aqi() : null;

        Map<String, String> g10 = pm10Grade(pm10);
        Map<String, String> g25 = pm25Grade(pm25);

        return new AirQualityResponse(
                lat,
                lng,
                pm10,
                pm25,
                aqi,
                g10.get("grade"),
                g25.get("grade"),
                g10.get("color"),
                g25.get("color"),
                cur != null ? cur.time() : null
        );
    }

    private Map<String, String> pm10Grade(Double v) {
        if (v == null) return Map.of("grade", "정보없음", "color", "#94a3b8");
        if (v <= 30) return Map.of("grade", "좋음", "color", "#0ea5e9");
        if (v <= 80) return Map.of("grade", "보통", "color", "#22c55e");
        if (v <= 150) return Map.of("grade", "나쁨", "color", "#f59e0b");
        return Map.of("grade", "매우나쁨", "color", "#dc2626");
    }

    private Map<String, String> pm25Grade(Double v) {
        if (v == null) return Map.of("grade", "정보없음", "color", "#94a3b8");
        if (v <= 15) return Map.of("grade", "좋음", "color", "#0ea5e9");
        if (v <= 35) return Map.of("grade", "보통", "color", "#22c55e");
        if (v <= 75) return Map.of("grade", "나쁨", "color", "#f59e0b");
        return Map.of("grade", "매우나쁨", "color", "#dc2626");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenMeteoAir(Current current) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Current(
            String time,
            Double pm10,
            Double pm2_5,
            Integer european_aqi
    ) {}
}