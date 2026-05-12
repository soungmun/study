package com.example.study.controller;

import com.example.study.dto.request.KakaoPayReadyRequest;
import com.example.study.dto.response.KakaoPayReadyResponse;
import com.example.study.entity.Payment;
import com.example.study.repository.PaymentRepository;
import com.example.study.service.KakaoPayService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/payments/kakao")
public class PaymentController {

    private final KakaoPayService kakaoPayService;
    private final PaymentRepository paymentRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public PaymentController(KakaoPayService kakaoPayService, PaymentRepository paymentRepository) {
        this.kakaoPayService = kakaoPayService;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/my")
    public List<Payment> myPayments(HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) return Collections.emptyList();
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @PostMapping("/ready")
    public KakaoPayReadyResponse ready(@Valid @RequestBody KakaoPayReadyRequest request, HttpSession session) {
        Long userId = currentUserId(session);
        return kakaoPayService.ready(request, userId);
    }

    @GetMapping("/approve")
    public ResponseEntity<Void> approve(
            @RequestParam("orderId") String orderId,
            @RequestParam("pg_token") String pgToken
    ) {
        URI redirect;
        try {
            Payment p = kakaoPayService.approve(orderId, pgToken);
            redirect = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/pay/success")
                    .queryParam("orderId", p.getPartnerOrderId())
                    .queryParam("amount", p.getTotalAmount())
                    .queryParam("itemName", p.getItemName())
                    .build()
                    .encode()
                    .toUri();
        } catch (Exception e) {
            redirect = UriComponentsBuilder.fromUriString(frontendUrl)
                    .path("/pay/fail")
                    .queryParam("reason", e.getMessage())
                    .build()
                    .encode()
                    .toUri();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirect.toString())
                .build();
    }

    @GetMapping("/cancel")
    public ResponseEntity<Void> cancel(@RequestParam("orderId") String orderId) {
        kakaoPayService.markCanceled(orderId);
        URI redirect = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/pay/fail")
                .queryParam("reason", "사용자가 결제를 취소했습니다.")
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirect.toString())
                .build();
    }

    @GetMapping("/fail")
    public ResponseEntity<Void> fail(@RequestParam("orderId") String orderId) {
        kakaoPayService.markFailed(orderId);
        URI redirect = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/pay/fail")
                .queryParam("reason", "결제에 실패했습니다.")
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirect.toString())
                .build();
    }

    private Long currentUserId(HttpSession session) {
        Object id = session.getAttribute(AuthController.SESSION_USER_KEY);
        return id instanceof Long u ? u : null;
    }
}