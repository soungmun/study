package com.example.study.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 결제 내역 데이터를 관리하는 엔티티 클래스입니다.
 * 사용자 결제 정보, 결제 상태, 주문 번호 등을 저장합니다.
 */
@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    public enum Status { READY, APPROVED, CANCELED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_order_id", nullable = false, length = 80)
    private String partnerOrderId;

    @Column(name = "partner_user_id", nullable = false, length = 80)
    private String partnerUserId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(length = 40)
    private String tid;

    @Column(length = 40)
    private String aid;

    @Column(name = "payment_method_type", length = 20)
    private String paymentMethodType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = Status.READY;
    }
}