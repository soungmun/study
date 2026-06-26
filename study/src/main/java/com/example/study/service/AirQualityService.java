package com.example.study.service;

import com.example.study.dto.response.AirQualityResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class AirQualityService {

    private static final Logger log = LoggerFactory.getLogger(AirQualityService.class);
    private static final String AIR_URL = "https://air-quality-api.open-meteo.com/v1/air-quality";

    private final RestClient restClient = RestClient.create();

    public AirQualityResponse getTomorrow(double lat, double lng) {
        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        URI uri = UriComponentsBuilder.fromUriString(AIR_URL)
                .queryParam("latitude", lat)
                .queryParam("longitude", lng)
                .queryParam("hourly", "pm10,pm2_5,european_aqi")
                .queryParam("timezone", "Asia/Seoul")
                .queryParam("start_date", tomorrow.toString())
                .queryParam("end_date", tomorrow.toString())
                .build()
                .toUri();

        OpenMeteoAir body = null;
        try {
            body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OpenMeteoAir.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch air quality data from Open-Meteo API for tomorrow (lat={}, lng={}): {}", lat, lng, e.getMessage());
            // 에러 발생 시 기본 응답 또는 null 반환하여 프론트엔드에서 처리하도록 함
            return AirQualityResponse.empty(lat, lng); // 새로운 헬퍼 메서드 추가 필요
        }


        Hourly h = body != null ? body.hourly() : null;
        Double pm10 = avg(h != null ? h.pm10() : null);
        Double pm25 = avg(h != null ? h.pm25() : null);
        Integer aqi = avgInt(h != null ? h.europeanAqi() : null);
        String time = (h != null && h.time() != null && !h.time().isEmpty()) ? h.time().get(0) : null;

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
                time
        );
    }

    private static Double avg(List<Double> list) {
        if (list == null || list.isEmpty()) return null;
        double sum = 0;
        int n = 0;
        for (Double v : list) {
            if (v != null) { sum += v; n++; }
        }
        return n == 0 ? null : sum / n;
    }

    private static Integer avgInt(List<Integer> list) {
        if (list == null || list.isEmpty()) return null;
        long sum = 0;
        int n = 0;
        for (Integer v : list) {
            if (v != null) { sum += v; n++; }
        }
        return n == 0 ? null : (int) Math.round((double) sum / n);
    }

    public AirQualityResponse get(double lat, double lng) {
        URI uri = UriComponentsBuilder.fromUriString(AIR_URL)
                .queryParam("latitude", lat)
                .queryParam("longitude", lng)
                .queryParam("current", "pm10,pm2_5,european_aqi")
                .queryParam("timezone", "Asia/Seoul")
                .build()
                .toUri();

        OpenMeteoAir body = null;
        try {
            body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OpenMeteoAir.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch air quality data from Open-Meteo API (lat={}, lng={}): {}", lat, lng, e.getMessage());
            // 에러 발생 시 기본 응답 또는 null 반환하여 프론트엔드에서 처리하도록 함
            return AirQualityResponse.empty(lat, lng); // 새로운 헬퍼 메서드 추가 필요
        }


        Current cur = body != null ? body.current() : null;
        Double pm10 = cur != null ? cur.pm10() : null;
        Double pm25 = cur != null ? cur.pm25() : null;
        Integer aqi = cur != null ? cur.europeanAqi() : null;

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
    public record OpenMeteoAir(Current current, Hourly hourly) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(
            String time,
            Double pm10,
            @JsonProperty("pm2_5") Double pm25,
            @JsonProperty("european_aqi") Integer europeanAqi
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hourly(
            List<String> time,
            List<Double> pm10,
            @JsonProperty("pm2_5") List<Double> pm25,
            @JsonProperty("european_aqi") List<Integer> europeanAqi
    ) {}
}