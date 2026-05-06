package com.example.study.service;

import com.example.study.dto.BookSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class BookService {

    private final RestClient restClient = RestClient.create();
    private final String apiKey;
    private final String bookSearchUrl;

    public BookService(
            @Value("${kakao.api.key}") String apiKey,
            @Value("${kakao.api.book-search-url}") String bookSearchUrl) {
        this.apiKey = apiKey;
        this.bookSearchUrl = bookSearchUrl;
    }

    public BookSearchResponse search(String query, String sort, String target, int page, int size) {
        if (query == null || query.isBlank()) {
            return new BookSearchResponse(
                    new BookSearchResponse.Meta(0, 0, true),
                    java.util.List.of()
            );
        }

        URI uri = UriComponentsBuilder.fromUriString(bookSearchUrl)
                .queryParam("query", query)
                .queryParam("sort", sort == null || sort.isBlank() ? "accuracy" : sort)
                .queryParamIfPresent("target", target == null || target.isBlank()
                        ? java.util.Optional.empty()
                        : java.util.Optional.of(target))
                .queryParam("page", Math.max(1, page))
                .queryParam("size", Math.min(50, Math.max(1, size)))
                .build()
                .encode()
                .toUri();

        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                .retrieve()
                .body(BookSearchResponse.class);
    }
}