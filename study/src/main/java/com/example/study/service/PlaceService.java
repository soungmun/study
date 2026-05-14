package com.example.study.service;

import com.example.study.dto.response.PlaceSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Service
public class PlaceService {

    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    private static final String SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final String CATEGORY_URL = "https://dapi.kakao.com/v2/local/search/category.json";
    private static final PlaceSearchResponse EMPTY =
            new PlaceSearchResponse(new PlaceSearchResponse.Meta(0, 0, true), List.of());

    private final RestClient restClient = RestClient.create();
    private final String apiKey;

    public PlaceService(@Value("${kakao.api.key}") String apiKey) {
        this.apiKey = apiKey;
    }

    public PlaceSearchResponse search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return new PlaceSearchResponse(
                    new PlaceSearchResponse.Meta(0, 0, true),
                    List.of()
            );
        }

        URI uri = UriComponentsBuilder.fromUriString(SEARCH_URL)
                .queryParam("query", query)
                .queryParam("page", Math.max(1, Math.min(45, page)))
                .queryParam("size", Math.max(1, Math.min(15, size)))
                .build()
                .encode()
                .toUri();

        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                .retrieve()
                .body(PlaceSearchResponse.class);
    }

    public PlaceSearchResponse searchNearby(double lat, double lng, String categoryGroupCode, int radius, int size) {
        if (categoryGroupCode == null || categoryGroupCode.isBlank()) {
            return EMPTY;
        }
        int clampedRadius = Math.max(1, Math.min(20000, radius));
        int clampedSize = Math.max(1, Math.min(15, size));

        URI uri = UriComponentsBuilder.fromUriString(CATEGORY_URL)
                .queryParam("category_group_code", categoryGroupCode)
                .queryParam("x", lng)
                .queryParam("y", lat)
                .queryParam("radius", clampedRadius)
                .queryParam("size", clampedSize)
                .queryParam("sort", "distance")
                .build()
                .encode()
                .toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                    .retrieve()
                    .body(PlaceSearchResponse.class);
        } catch (HttpClientErrorException.BadRequest e) {
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("API limit has been exceeded")) {
                log.warn("[PlaceService] 카카오 로컬 API 일일 쿼터 초과 — 빈 결과 반환");
                return EMPTY;
            }
            log.warn("[PlaceService] 카카오 카테고리 검색 400: {}", body);
            return EMPTY;
        } catch (RestClientResponseException e) {
            log.warn("[PlaceService] 카카오 카테고리 검색 실패 status={}: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return EMPTY;
        }
    }
}