package com.example.study.repository;

import com.example.study.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPartnerOrderId(String partnerOrderId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}