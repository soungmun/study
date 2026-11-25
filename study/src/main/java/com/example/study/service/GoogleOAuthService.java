package com.example.study.service;

import lombok.extern.slf4j.Slf4j;
import com.example.study.dto.response.GoogleTokenResponse;
import com.example.study.dto.response.GoogleUserResponse;
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

/**
 * 구글 소셜 로그인 연동 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 구글 OAuth API와의 통신 및 토큰 갱신, 사용자 정보 조회를 수행합니다.
 */
@Slf4j
@Service
public class GoogleOAuthService {

    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_URL = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String SCOPE = "openid email profile";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleOAuthService(
            @Value("${google.oauth.client-id}") String clientId,
            @Value("${google.oauth.client-secret}") String clientSecret,
            @Value("${google.oauth.redirect-uri}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /**
     * 구글 OAuth 인증 페이지 URL을 생성합니다.
     *
     * @param state CSRF 방지용 state 값 (null 가능)
     * @return 구글 로그인 페이지 URL
     */
    public String authorizeUrl(String state) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", SCOPE)
                .queryParam("access_type", "online")
                .queryParam("prompt", "select_account");
        if (state != null && !state.isBlank()) {
            b.queryParam("state", state);
        }
        return b.build().toUriString();
    }

    /**
     * 구글 인증 코드를 액세스 토큰으로 교환합니다.
     *
     * @param code 구글 OAuth 인증 코드
     * @return 구글 토큰 응답 (access_token, id_token 등)
     */
    public GoogleTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        ResponseEntity<String> response = restClient.post()
                .uri(URI.create(TOKEN_URL))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toEntity(String.class);

        String body = response.getBody();
        log.info("[Google token] status={} bodyLen={}",
                response.getStatusCode(),
                body == null ? 0 : body.length());

        try {
            GoogleTokenResponse parsed = objectMapper.readValue(body, GoogleTokenResponse.class);
            if (parsed.error() != null && !parsed.error().isBlank()) {
                throw new IllegalStateException(
                        "구글 토큰 응답 에러: " + parsed.error() + " " + parsed.errorDescription());
            }
            return parsed;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "구글 토큰 응답 파싱 실패: " + e.getMessage() + " body=" + body, e);
        }
    }

    /**
     * 구글 액세스 토큰으로 사용자 프로필 정보를 조회합니다.
     *
     * @param accessToken 구글 액세스 토큰
     * @return 구글 사용자 정보 (sub, email, name, picture 등)
     */
    public GoogleUserResponse fetchUser(String accessToken) {
        return restClient.get()
                .uri(URI.create(USER_URL))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserResponse.class);
    }
}