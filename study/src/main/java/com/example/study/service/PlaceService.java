package com.example.study.service;

import com.example.study.dto.response.PlaceSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Service
public class PlaceService {

    private static final String SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

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
}