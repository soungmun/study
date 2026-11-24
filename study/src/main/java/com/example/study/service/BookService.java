package com.example.study.service;

import com.example.study.dto.response.BookSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 도서 검색 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 외부 API(예: 카카오 도서검색)를 사용하여 책 정보를 검색하고 자동완성을 제공합니다.
 */
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

    /**
     * 카카오 도서 검색 API를 호출하여 책 목록을 반환합니다.
     *
     * @param query  검색어
     * @param sort   정렬 기준 (accuracy: 정확도순, latest: 최신순)
     * @param target 검색 대상 필드 (title, isbn, publisher 등, null이면 전체)
     * @param page   페이지 번호 (1부터 시작)
     * @param size   페이지당 결과 수 (최대 50)
     * @return 검색 결과 DTO (도서 목록 + 메타 정보)
     */
    public BookSearchResponse search(String query, String sort, String target, int page, int size) {
        if (query == null || query.isBlank()) {
            return new BookSearchResponse(
                    new BookSearchResponse.Meta(0, 0, true),
                    List.of()
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

    /**
     * 검색어로 도서 제목 자동완성 목록을 반환합니다.
     * 최대 10개의 중복 없는 제목 목록을 반환하며, 오류 발생 시 빈 리스트를 반환합니다.
     *
     * @param query 자동완성 검색어
     * @return 도서 제목 목록 (최대 10개)
     */
    public List<String> autocomplete(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        URI uri = UriComponentsBuilder.fromUriString(bookSearchUrl)
                .queryParam("query", query)
                .queryParam("sort", "accuracy")
                .queryParam("page", 1)
                .queryParam("size", 10)
                .build()
                .encode()
                .toUri();

        try {
            BookSearchResponse response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                    .retrieve()
                    .body(BookSearchResponse.class);

            if (response == null || response.documents() == null) {
                return List.of();
            }

            return response.documents().stream()
                    .map(b -> b.title())
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }
}
