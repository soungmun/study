package com.example.study.repository;

import com.example.study.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 주문 ID로 결제 조회 (카카오페이 결제 승인/취소 시 사용)
    // SQL: SELECT * FROM payment WHERE partner_order_id = ?
    Optional<Payment> findByPartnerOrderId(String partnerOrderId);

    // 특정 사용자의 결제 내역 최신순 조회
    // SQL: SELECT * FROM payment WHERE user_id = ? ORDER BY created_at DESC
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
