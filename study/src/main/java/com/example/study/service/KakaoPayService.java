package com.example.study.service;

import com.example.study.dto.request.KakaoPayReadyRequest;
import com.example.study.dto.response.KakaoPayReadyResponse;
import com.example.study.entity.Payment;
import com.example.study.repository.PaymentRepository;
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

/**
 * 카카오페이 결제 연동 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 결제 준비, 승인, 취소 처리 등의 외부 결제 API 통신을 수행합니다.
 */
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

    /**
     * 카카오페이 결제 준비를 요청합니다.
     * DB에 결제 레코드를 생성하고, 카카오페이로부터 결제 페이지 URL(tid)을 받아 반환합니다.
     *
     * @param req    결제 준비 요청 (상품명, 수량, 금액)
     * @param userId 결제 요청 사용자 ID (비회원이면 null)
     * @return 카카오페이 결제 준비 응답 (tid, 결제 페이지 URL 등)
     */
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

        Map<String, Object> res;
        try {
            res = restClient.post()
                    .uri(apiBase + "/online/v1/payment/ready")
                    .header("Authorization", "SECRET_KEY " + secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (HttpStatusCodeException e) {
            String respBody = e.getResponseBodyAsString();
            log.error("[KakaoPay] ready 실패 status={} body={}", e.getStatusCode(), respBody);
            throw new IllegalStateException("카카오페이 결제 준비 실패 (" + e.getStatusCode() + "): " + respBody);
        }
        log.info("[KakaoPay] ready 응답: {}", res);
        if (res == null) {
            throw new IllegalStateException("카카오페이 응답이 비어 있습니다.");
        }
        String tid = str(res, "tid");
        String pcUrl = str(res, "next_redirect_pc_url", "nextRedirectPcUrl");
        String mobileUrl = str(res, "next_redirect_mobile_url", "nextRedirectMobileUrl");
        String appUrl = str(res, "next_redirect_app_url", "nextRedirectAppUrl");
        String androidScheme = str(res, "android_app_scheme", "androidAppScheme");
        String iosScheme = str(res, "ios_app_scheme", "iosAppScheme");
        if (tid == null) {
            throw new IllegalStateException("카카오페이 응답에 tid가 없습니다: " + res);
        }

        payment.setTid(tid);
        paymentRepository.save(payment);

        return new KakaoPayReadyResponse(
                payment.getId(),
                tid,
                partnerOrderId,
                pcUrl,
                mobileUrl,
                appUrl,
                androidScheme,
                iosScheme
        );
    }

    private static String str(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null) return v.toString();
        }
        return null;
    }

    /**
     * 카카오페이 결제 승인을 처리합니다.
     * 결제 성공 시 Payment 상태를 APPROVED로 변경합니다.
     *
     * @param partnerOrderId 주문 ID (ready 단계에서 생성된 고유 ID)
     * @param pgToken        카카오페이 결제 완료 후 전달받는 pg_token
     * @return 승인 완료된 Payment 엔티티
     */
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

        Map<String, Object> res;
        try {
            res = restClient.post()
                    .uri(apiBase + "/online/v1/payment/approve")
                    .header("Authorization", "SECRET_KEY " + secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (HttpStatusCodeException e) {
            String respBody = e.getResponseBodyAsString();
            log.error("[KakaoPay] approve 실패 status={} body={}", e.getStatusCode(), respBody);
            payment.setStatus(Payment.Status.FAILED);
            paymentRepository.save(payment);
            throw new IllegalStateException("카카오페이 결제 승인 실패 (" + e.getStatusCode() + "): " + respBody);
        }
        log.info("[KakaoPay] approve 응답: {}", res);

        payment.setStatus(Payment.Status.APPROVED);
        payment.setAid(str(res, "aid"));
        payment.setPaymentMethodType(str(res, "payment_method_type", "paymentMethodType"));
        payment.setApprovedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    /**
     * 결제를 취소 상태(CANCELED)로 변경합니다.
     *
     * @param partnerOrderId 취소할 주문 ID
     * @return 상태가 변경된 Payment, 없으면 null
     */
    @Transactional
    public Payment markCanceled(String partnerOrderId) {
        return paymentRepository.findByPartnerOrderId(partnerOrderId)
                .map(p -> { p.setStatus(Payment.Status.CANCELED); return paymentRepository.save(p); })
                .orElse(null);
    }

    /**
     * 결제를 실패 상태(FAILED)로 변경합니다.
     *
     * @param partnerOrderId 실패 처리할 주문 ID
     * @return 상태가 변경된 Payment, 없으면 null
     */
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

}