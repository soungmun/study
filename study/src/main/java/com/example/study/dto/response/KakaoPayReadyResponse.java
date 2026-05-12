package com.example.study.dto.response;

public record KakaoPayReadyResponse(
        Long paymentId,
        String tid,
        String partnerOrderId,
        String nextRedirectPcUrl,
        String nextRedirectMobileUrl,
        String nextRedirectAppUrl,
        String androidAppScheme,
        String iosAppScheme
) {}
