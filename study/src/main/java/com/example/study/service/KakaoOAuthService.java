package com.example.study.service;

import lombok.extern.slf4j.Slf4j;
import com.example.study.dto.response.KakaoTokenResponse;
import com.example.study.dto.response.KakaoUserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 카카오 소셜 로그인 연동 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 카카오 OAuth API와의 통신 및 토큰 발급, 사용자 프로필 조회를 수행합니다.
 */
@Slf4j
@Service
public class KakaoOAuthService {

    private static final String AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoOAuthService(
            @Value("${kakao.oauth.client-id}") String clientId,
            @Value("${kakao.oauth.client-secret:}") String clientSecret,
            @Value("${kakao.oauth.redirect-uri}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /**
     * 카카오 OAuth 인증 페이지 URL을 생성합니다.
     *
     * @param state CSRF 방지용 state 값 (null 가능)
     * @return 카카오 로그인 페이지 URL
     */
    public String authorizeUrl(String state) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "talk_message");
        if (state != null && !state.isBlank()) {
            b.queryParam("state", URLEncoder.encode(state, StandardCharsets.UTF_8));
        }
        return b.build().toUriString();
    }

    /**
     * 카카오 리프레시 토큰으로 새로운 액세스 토큰을 발급받습니다.
     *
     * @param refreshToken 카카오 리프레시 토큰
     * @return 새로운 토큰 응답
     */
    public KakaoTokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("refresh_token", refreshToken);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        ResponseEntity<String> response = restClient.post()
                .uri(URI.create(TOKEN_URL))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toEntity(String.class);

        String body = response.getBody();
        log.info("[Kakao refresh] status={} bodyLen={}", response.getStatusCode(),
                body == null ? 0 : body.length());
        try {
            return objectMapper.readValue(body, KakaoTokenResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "카카오 refresh 응답 파싱 실패: " + e.getMessage() + " body=" + body, e);
        }
    }

    /**
     * 카카오 인증 코드를 액세스 토큰으로 교환합니다.
     *
     * @param code 카카오 OAuth 인증 코드
     * @return 카카오 토큰 응답 (access_token, refresh_token 등)
     */
    public KakaoTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        URI uri = URI.create(TOKEN_URL);

        ResponseEntity<String> response = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toEntity(String.class);

        String body = response.getBody();
        log.info("[Kakao token] status={} bodyLen={} body={}",
                response.getStatusCode(),
                body == null ? 0 : body.length(),
                body);

        try {
            return objectMapper.readValue(body, KakaoTokenResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "카카오 토큰 응답 파싱 실패: " + e.getMessage() + " body=" + body, e);
        }
    }

    /**
     * 카카오 액세스 토큰으로 사용자 프로필 정보를 조회합니다.
     *
     * @param accessToken 카카오 액세스 토큰
     * @return 카카오 사용자 정보 (id, 이메일, 닉네임 등)
     */
    public KakaoUserResponse fetchUser(String accessToken) {
        return restClient.get()
                .uri(URI.create(USER_URL))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserResponse.class);
    }
}