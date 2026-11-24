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
import java.util.stream.Collectors;

/**
 * 장소 검색 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 카카오 로컬 API 등을 사용하여 장소 키워드 검색 및 주변 장소 정보를 제공합니다.
 */
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

    /**
     * 카카오 로컬 API를 사용하여 키워드로 장소를 검색합니다.
     *
     * @param query 검색 키워드
     * @param page  페이지 번호 (1~45)
     * @param size  페이지당 결과 수 (1~15)
     * @return 장소 검색 결과 DTO
     */
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

    /**
     * 장소 이름 자동완성 목록을 반환합니다. 최대 10개의 중복 없는 장소명을 반환합니다.
     *
     * @param query 자동완성 검색어
     * @return 장소 이름 목록 (최대 10개, 오류 시 빈 리스트)
     */
    public List<String> autocomplete(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        URI uri = UriComponentsBuilder.fromUriString(SEARCH_URL)
                .queryParam("query", query)
                .queryParam("page", 1)
                .queryParam("size", 10)
                .build()
                .encode()
                .toUri();

        try {
            PlaceSearchResponse response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                    .retrieve()
                    .body(PlaceSearchResponse.class);

            if (response == null || response.documents() == null) {
                return List.of();
            }

            return response.documents().stream()
                    .map(PlaceSearchResponse.Document::placeName)
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 특정 좌표 주변의 카테고리별 장소를 검색합니다.
     * 카테고리 API 쿼터 초과 등 실패 시 키워드 검색으로 자동 폴백합니다.
     *
     * @param lat                 중심 위도
     * @param lng                 중심 경도
     * @param categoryGroupCode   카카오 카테고리 그룹 코드 (예: FD6=음식점, CE7=카페)
     * @param radius              검색 반경 (미터, 최대 20000)
     * @param size                결과 수 (최대 15)
     * @return 주변 장소 검색 결과 DTO
     */
    public PlaceSearchResponse searchNearby(double lat, double lng, String categoryGroupCode, int radius, int size) {
        if (categoryGroupCode == null || categoryGroupCode.isBlank()) {
            return EMPTY;
        }
        PlaceSearchResponse byCategory = searchByCategory(lat, lng, categoryGroupCode, radius, size);
        if (byCategory.documents() != null && !byCategory.documents().isEmpty()) {
            return byCategory;
        }
        // 카테고리 API 쿼터 초과 등으로 결과가 비면 키워드 검색으로 폴백
        String fallbackQuery = "FD6".equalsIgnoreCase(categoryGroupCode) ? "맛집" : "주변";
        return searchByKeyword(fallbackQuery, lat, lng, categoryGroupCode, radius, size);
    }

    private PlaceSearchResponse searchByCategory(double lat, double lng, String categoryGroupCode, int radius, int size) {
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
                log.warn("[PlaceService] 카카오 카테고리 API 쿼터 초과 — 키워드 검색 폴백");
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

    private PlaceSearchResponse searchByKeyword(String query, double lat, double lng, String categoryGroupCode, int radius, int size) {
        int clampedRadius = Math.max(1, Math.min(20000, radius));
        int clampedSize = Math.max(1, Math.min(15, size));

        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(SEARCH_URL)
                .queryParam("query", query)
                .queryParam("x", lng)
                .queryParam("y", lat)
                .queryParam("radius", clampedRadius)
                .queryParam("size", clampedSize)
                .queryParam("sort", "distance");
        if (categoryGroupCode != null && !categoryGroupCode.isBlank()) {
            b.queryParam("category_group_code", categoryGroupCode);
        }
        URI uri = b.build().encode().toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                    .retrieve()
                    .body(PlaceSearchResponse.class);
        } catch (RestClientResponseException e) {
            log.warn("[PlaceService] 카카오 키워드 폴백 실패 status={}: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return EMPTY;
        }
    }
}
