package com.example.study.service;

import com.example.study.dto.response.WeatherResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private static final String WEATHER_URL = "https://api.open-meteo.com/v1/forecast";

    private final RestClient restClient = RestClient.create();

    public WeatherResponse getTomorrow(double lat, double lng) {
        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        URI uri = UriComponentsBuilder.fromUriString(WEATHER_URL)
                .queryParam("latitude", lat)
                .queryParam("longitude", lng)
                .queryParam("daily", "temperature_2m_max,temperature_2m_min,weathercode")
                .queryParam("timezone", "Asia/Seoul")
                .queryParam("start_date", tomorrow.toString())
                .queryParam("end_date", tomorrow.toString())
                .build()
                .toUri();

        OpenMeteoResponse body = restClient.get()
                .uri(uri)
                .retrieve()
                .body(OpenMeteoResponse.class);

        OpenMeteoDaily d = body != null ? body.daily() : null;
        Double tMax = first(d != null ? d.temperatureMax() : null);
        Double tMin = first(d != null ? d.temperatureMin() : null);
        Integer code = firstInt(d != null ? d.weathercode() : null);
        String time = firstString(d != null ? d.time() : null);

        Double avg = (tMax != null && tMin != null) ? (tMax + tMin) / 2.0 : (tMax != null ? tMax : tMin);
        Map<String, String> meta = describe(code);
        String desc = meta.get("description");
        if (tMax != null && tMin != null) {
            desc = String.format("%s · 최저 %.1f°C / 최고 %.1f°C", desc, tMin, tMax);
        }

        return new WeatherResponse(
                lat,
                lng,
                avg,
                null,
                null,
                code,
                desc,
                meta.get("icon"),
                time
        );
    }

    private static Double first(List<Double> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private static Integer firstInt(List<Integer> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private static String firstString(List<String> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public WeatherResponse getWeather(double lat, double lng) {
        URI uri = UriComponentsBuilder.fromUriString(WEATHER_URL)
                .queryParam("latitude", lat)
                .queryParam("longitude", lng)
                .queryParam("current_weather", true)
                .queryParam("timezone", "Asia/Seoul")
                .build()
                .toUri();

        OpenMeteoResponse body = restClient.get()
                .uri(uri)
                .retrieve()
                .body(OpenMeteoResponse.class);

        OpenMeteoCurrent cw = body != null ? body.currentWeather() : null;
        Integer code = cw != null ? cw.weathercode() : null;
        Map<String, String> meta = describe(code);

        return new WeatherResponse(
                lat,
                lng,
                cw != null ? cw.temperature() : null,
                cw != null ? cw.windspeed() : null,
                cw != null ? cw.winddirection() : null,
                code,
                meta.get("description"),
                meta.get("icon"),
                cw != null ? cw.time() : null
        );
    }

    private Map<String, String> describe(Integer code) {
        if (code == null) return Map.of("description", "정보 없음", "icon", "❔");
        return switch (code) {
            case 0 -> Map.of("description", "맑음", "icon", "☀️");
            case 1 -> Map.of("description", "대체로 맑음", "icon", "🌤️");
            case 2 -> Map.of("description", "부분 흐림", "icon", "⛅");
            case 3 -> Map.of("description", "흐림", "icon", "☁️");
            case 45, 48 -> Map.of("description", "안개", "icon", "🌫️");
            case 51, 53, 55 -> Map.of("description", "이슬비", "icon", "🌦️");
            case 56, 57 -> Map.of("description", "어는 이슬비", "icon", "🌧️");
            case 61 -> Map.of("description", "약한 비", "icon", "🌦️");
            case 63 -> Map.of("description", "비", "icon", "🌧️");
            case 65 -> Map.of("description", "강한 비", "icon", "🌧️");
            case 66, 67 -> Map.of("description", "어는 비", "icon", "🌧️");
            case 71 -> Map.of("description", "약한 눈", "icon", "🌨️");
            case 73 -> Map.of("description", "눈", "icon", "❄️");
            case 75 -> Map.of("description", "강한 눈", "icon", "❄️");
            case 77 -> Map.of("description", "싸락눈", "icon", "🌨️");
            case 80, 81, 82 -> Map.of("description", "소나기", "icon", "🌧️");
            case 85, 86 -> Map.of("description", "눈 소나기", "icon", "🌨️");
            case 95 -> Map.of("description", "천둥번개", "icon", "⛈️");
            case 96, 99 -> Map.of("description", "우박을 동반한 천둥번개", "icon", "⛈️");
            default -> Map.of("description", "정보 없음", "icon", "❔");
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenMeteoResponse(
            @JsonProperty("current_weather") OpenMeteoCurrent currentWeather,
            OpenMeteoDaily daily
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenMeteoCurrent(
            Double temperature,
            Double windspeed,
            Integer winddirection,
            Integer weathercode,
            String time
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenMeteoDaily(
            List<String> time,
            @JsonProperty("temperature_2m_max") List<Double> temperatureMax,
            @JsonProperty("temperature_2m_min") List<Double> temperatureMin,
            List<Integer> weathercode
    ) {}
}