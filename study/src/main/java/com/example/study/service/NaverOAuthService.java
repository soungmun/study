package com.example.study.service;

import lombok.extern.slf4j.Slf4j;
import com.example.study.dto.response.NaverTokenResponse;
import com.example.study.dto.response.NaverUserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 네이버 소셜 로그인 연동 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 네이버 OAuth API와의 통신 및 토큰 발급, 사용자 정보 조회를 수행합니다.
 */
@Slf4j
@Service
public class NaverOAuthService {

    private static final String AUTHORIZE_URL = "https://nid.naver.com/oauth2.0/authorize";
    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_URL = "https://openapi.naver.com/v1/nid/me";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public NaverOAuthService(
            @Value("${naver.oauth.client-id}") String clientId,
            @Value("${naver.oauth.client-secret}") String clientSecret,
            @Value("${naver.oauth.redirect-uri}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /**
     * 네이버 OAuth 인증 페이지 URL을 생성합니다.
     *
     * @param state CSRF 방지용 state 값
     * @return 네이버 로그인 페이지 URL
     */
    public String authorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
                .queryParam("state", URLEncoder.encode(state, StandardCharsets.UTF_8))
                .build(true)
                .toUriString();
    }

    /**
     * 네이버 인증 코드를 액세스 토큰으로 교환합니다.
     *
     * @param code  네이버 OAuth 인증 코드
     * @param state CSRF 방지용 state 값
     * @return 네이버 토큰 응답 (access_token 등)
     */
    public NaverTokenResponse exchangeCode(String code, String state) {
        URI uri = UriComponentsBuilder.fromUriString(TOKEN_URL)
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("code", code)
                .queryParam("state", URLEncoder.encode(state, StandardCharsets.UTF_8))
                .build(true)
                .toUri();

        ResponseEntity<String> response = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .toEntity(String.class);

        String body = response.getBody();
        log.info("[Naver token] status={} bodyLen={} body={}",
                response.getStatusCode(),
                body == null ? 0 : body.length(),
                body);

        try {
            NaverTokenResponse parsed = objectMapper.readValue(body, NaverTokenResponse.class);
            if (parsed.error() != null && !parsed.error().isBlank()) {
                throw new IllegalStateException(
                        "네이버 토큰 응답 에러: " + parsed.error() + " " + parsed.errorDescription());
            }
            return parsed;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "네이버 토큰 응답 파싱 실패: " + e.getMessage() + " body=" + body, e);
        }
    }

    /**
     * 네이버 액세스 토큰으로 사용자 프로필 정보를 조회합니다.
     *
     * @param accessToken 네이버 액세스 토큰
     * @return 네이버 사용자 정보 (id, 이메일, 닉네임 등)
     */
    public NaverUserResponse fetchUser(String accessToken) {
        return restClient.get()
                .uri(URI.create(USER_URL))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(NaverUserResponse.class);
    }
}