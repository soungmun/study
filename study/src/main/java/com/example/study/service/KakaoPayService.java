package com.example.study.service;

import com.example.study.dto.request.KakaoPayReadyRequest;
import com.example.study.dto.response.KakaoPayReadyResponse;
import com.example.study.entity.Payment;
import com.example.study.repository.PaymentRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class KakaoPayService {

    private static final Logger log = LoggerFactory.getLogger(KakaoPayService.class);

    private final PaymentRepository paymentRepository;
    private final RestClient restClient = RestClient.create();

    @Value("${kakaopay.api-base}")
    private String apiBase;

    @Value("${kakaopay.cid}")
    private String cid;

    @Value("${kakaopay.secret-key:}")
    private String secretKey;

    @Value("${kakaopay.approval-url}")
    private String approvalUrl;

    @Value("${kakaopay.cancel-url}")
    private String cancelUrl;

    @Value("${kakaopay.fail-url}")
    private String failUrl;

    public KakaoPayService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public KakaoPayReadyResponse ready(KakaoPayReadyRequest req, Long userId) {
        ensureSecretConfigured();

        String partnerOrderId = "ORDER-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String partnerUserId = userId != null ? "USER-" + userId : "GUEST-" + UUID.randomUUID().toString().substring(0, 8);

        Payment payment = new Payment();
        payment.setPartnerOrderId(partnerOrderId);
        payment.setPartnerUserId(partnerUserId);
        payment.setUserId(userId);
        payment.setItemName(req.itemName());
        payment.setQuantity(req.quantity());
        payment.setTotalAmount(req.totalAmount());
        payment.setStatus(Payment.Status.READY);
        paymentRepository.save(payment);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", cid);
        body.put("partner_order_id", partnerOrderId);
        body.put("partner_user_id", partnerUserId);
        body.put("item_name", req.itemName());
        body.put("quantity", req.quantity());
        body.put("total_amount", req.totalAmount());
        body.put("tax_free_amount", 0);
        body.put("approval_url", approvalUrl + "?orderId=" + partnerOrderId);
        body.put("cancel_url", cancelUrl + "?orderId=" + partnerOrderId);
        body.put("fail_url", failUrl + "?orderId=" + partnerOrderId);

        ReadyApiResponse res;
        try {
            res = restClient.post()
                    .uri(apiBase + "/online/v1/payment/ready")
                    .header("Authorization", "SECRET_KEY " + secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ReadyApiResponse.class);
        } catch (HttpStatusCodeException e) {
            log.error("[KakaoPay] ready 실패 status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("카카오페이 결제 준비에 실패했습니다: " + e.getStatusCode());
        }
        if (res == null || res.tid() == null) {
            throw new IllegalStateException("카카오페이 응답이 비어 있습니다.");
        }

        payment.setTid(res.tid());
        paymentRepository.save(payment);

        return new KakaoPayReadyResponse(
                payment.getId(),
                res.tid(),
                partnerOrderId,
                res.nextRedirectPcUrl(),
                res.nextRedirectMobileUrl(),
                res.nextRedirectAppUrl(),
                res.androidAppScheme(),
                res.iosAppScheme()
        );
    }

    @Transactional
    public Payment approve(String partnerOrderId, String pgToken) {
        ensureSecretConfigured();

        Payment payment = paymentRepository.findByPartnerOrderId(partnerOrderId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 없습니다: " + partnerOrderId));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cid", cid);
        body.put("tid", payment.getTid());
        body.put("partner_order_id", payment.getPartnerOrderId());
        body.put("partner_user_id", payment.getPartnerUserId());
        body.put("pg_token", pgToken);

        ApproveApiResponse res;
        try {
            res = restClient.post()
                    .uri(apiBase + "/online/v1/payment/approve")
                    .header("Authorization", "SECRET_KEY " + secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ApproveApiResponse.class);
        } catch (HttpStatusCodeException e) {
            log.error("[KakaoPay] approve 실패 status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            payment.setStatus(Payment.Status.FAILED);
            paymentRepository.save(payment);
            throw new IllegalStateException("카카오페이 결제 승인에 실패했습니다: " + e.getStatusCode());
        }

        payment.setStatus(Payment.Status.APPROVED);
        payment.setAid(res != null ? res.aid() : null);
        payment.setPaymentMethodType(res != null ? res.paymentMethodType() : null);
        payment.setApprovedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment markCanceled(String partnerOrderId) {
        return paymentRepository.findByPartnerOrderId(partnerOrderId)
                .map(p -> { p.setStatus(Payment.Status.CANCELED); return paymentRepository.save(p); })
                .orElse(null);
    }

    @Transactional
    public Payment markFailed(String partnerOrderId) {
        return paymentRepository.findByPartnerOrderId(partnerOrderId)
                .map(p -> { p.setStatus(Payment.Status.FAILED); return paymentRepository.save(p); })
                .orElse(null);
    }

    private void ensureSecretConfigured() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "KAKAOPAY_SECRET_KEY가 설정되어 있지 않습니다. application.properties 또는 환경변수로 카카오페이 시크릿 키를 주입하세요.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ReadyApiResponse(
            String tid,
            String nextRedirectPcUrl,
            String nextRedirectMobileUrl,
            String nextRedirectAppUrl,
            String androidAppScheme,
            String iosAppScheme,
            String createdAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ApproveApiResponse(
            String aid,
            String tid,
            String cid,
            @JsonProperty("partner_order_id") String partnerOrderId,
            @JsonProperty("partner_user_id") String partnerUserId,
            @JsonProperty("payment_method_type") String paymentMethodType,
            String itemName,
            Integer quantity,
            String createdAt,
            String approvedAt
    ) {}
}