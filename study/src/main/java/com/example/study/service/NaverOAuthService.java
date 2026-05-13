package com.example.study.service;

import com.example.study.dto.response.NaverTokenResponse;
import com.example.study.dto.response.NaverUserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class NaverOAuthService {

    private static final Logger log = LoggerFactory.getLogger(NaverOAuthService.class);

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

    public String authorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
                .queryParam("state", URLEncoder.encode(state, StandardCharsets.UTF_8))
                .build(true)
                .toUriString();
    }

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

    public NaverUserResponse fetchUser(String accessToken) {
        return restClient.get()
                .uri(URI.create(USER_URL))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(NaverUserResponse.class);
    }
}