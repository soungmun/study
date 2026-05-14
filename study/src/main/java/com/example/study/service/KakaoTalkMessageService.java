package com.example.study.service;

import com.example.study.dto.response.KakaoTokenResponse;
import com.example.study.entity.User;
import com.example.study.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class KakaoTalkMessageService {

    private static final Logger log = LoggerFactory.getLogger(KakaoTalkMessageService.class);
    private static final String SEND_URL = "https://kapi.kakao.com/v2/api/talk/memo/default/send";
    private static final int TEXT_MAX = 200;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KakaoOAuthService kakaoOAuth;
    private final UserRepository userRepository;
    private final String frontendUrl;

    public KakaoTalkMessageService(
            KakaoOAuthService kakaoOAuth,
            UserRepository userRepository,
            @Value("${app.frontend.url}") String frontendUrl
    ) {
        this.kakaoOAuth = kakaoOAuth;
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
    }

    /** 사용자에게 "나에게 보내기"로 텍스트 메시지 전송. 토큰 만료 시 자동 refresh. */
    public boolean sendTextToSelf(User user, String text, String linkUrl) {
        if (user.getKakaoId() == null) return false;
        if (!user.isKakaoTalkOptIn()) return false;
        String token = ensureFreshToken(user);
        if (token == null) return false;

        String trimmed = text == null ? "" : (text.length() > TEXT_MAX ? text.substring(0, TEXT_MAX) : text);
        String link = (linkUrl == null || linkUrl.isBlank()) ? frontendUrl : linkUrl;

        try {
            return doSend(token, trimmed, link);
        } catch (UnauthorizedException e) {
            log.info("[KakaoTalk] 401 — refresh 후 1회 재시도 userId={}", user.getId());
            String refreshed = forceRefresh(user);
            if (refreshed == null) return false;
            try {
                return doSend(refreshed, trimmed, link);
            } catch (Exception e2) {
                log.warn("[KakaoTalk] 재시도 실패 userId={}: {}", user.getId(), e2.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.warn("[KakaoTalk] 발송 실패 userId={}: {}", user.getId(), e.getMessage());
            return false;
        }
    }

    private boolean doSend(String accessToken, String text, String link) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("object_type", "text");
        template.put("text", text);
        Map<String, String> linkObj = new LinkedHashMap<>();
        linkObj.put("web_url", link);
        linkObj.put("mobile_web_url", link);
        template.put("link", linkObj);
        template.put("button_title", "사이트 열기");

        String templateJson;
        try {
            templateJson = objectMapper.writeValueAsString(template);
        } catch (Exception e) {
            throw new IllegalStateException("template_object 직렬화 실패", e);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("template_object", templateJson);

        ResponseEntity<String> response = restClient.post()
                .uri(URI.create(SEND_URL))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    String body = new String(res.getBody().readAllBytes());
                    if (res.getStatusCode().value() == 401) {
                        throw new UnauthorizedException(body);
                    }
                    throw new IllegalStateException("[KakaoTalk] " + res.getStatusCode() + " " + body);
                })
                .toEntity(String.class);

        log.info("[KakaoTalk] OK status={} body={}", response.getStatusCode(), response.getBody());
        return response.getStatusCode().is2xxSuccessful();
    }

    private String ensureFreshToken(User user) {
        String at = user.getKakaoAccessToken();
        LocalDateTime exp = user.getKakaoTokenExpiresAt();
        if (at == null || at.isBlank()) return null;
        if (exp != null && LocalDateTime.now().plusMinutes(1).isAfter(exp)) {
            return forceRefresh(user);
        }
        return at;
    }

    private String forceRefresh(User user) {
        String rt = user.getKakaoRefreshToken();
        if (rt == null || rt.isBlank()) {
            log.info("[KakaoTalk] refresh token 없음 userId={}", user.getId());
            return null;
        }
        try {
            KakaoTokenResponse refreshed = kakaoOAuth.refreshToken(rt);
            if (refreshed == null || refreshed.accessToken() == null) {
                log.warn("[KakaoTalk] refresh 응답 비정상 userId={}", user.getId());
                return null;
            }
            user.setKakaoAccessToken(refreshed.accessToken());
            if (refreshed.expiresIn() != null) {
                user.setKakaoTokenExpiresAt(LocalDateTime.now().plusSeconds(refreshed.expiresIn()));
            }
            if (refreshed.refreshToken() != null && !refreshed.refreshToken().isBlank()) {
                user.setKakaoRefreshToken(refreshed.refreshToken());
            }
            userRepository.save(user);
            return refreshed.accessToken();
        } catch (Exception e) {
            log.warn("[KakaoTalk] refresh 실패 userId={}: {}", user.getId(), e.getMessage());
            return null;
        }
    }

    private static class UnauthorizedException extends RuntimeException {
        UnauthorizedException(String body) { super(body); }
    }
}