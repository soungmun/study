package com.example.study.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 시스템 점검 모드 관련 상태를 관리하는 서비스 클래스입니다.
 * 점검 시작/종료 처리 및 현재 점검 상태를 메모리에 캐싱하여 제공합니다.
 */
@Service
public class MaintenanceService {

    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private volatile String lastEnabledBy;
    private volatile LocalDateTime lastEnabledAt;
    private volatile LocalDateTime lastDisabledAt;

    /** 현재 점검 모드 활성화 여부를 반환합니다. */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * 점검 모드를 활성화합니다. 이미 활성화 상태이면 false를 반환합니다.
     *
     * @param byUsername 점검을 시작한 관리자 아이디
     * @return 새로 활성화되면 true, 이미 활성화 상태이면 false
     */
    public synchronized boolean enable(String byUsername) {
        if (enabled.get()) return false;
        enabled.set(true);
        lastEnabledBy = byUsername;
        lastEnabledAt = LocalDateTime.now();
        return true;
    }

    /**
     * 점검 모드를 비활성화합니다. 이미 비활성화 상태이면 false를 반환합니다.
     *
     * @return 새로 비활성화되면 true, 이미 비활성화 상태이면 false
     */
    public synchronized boolean disable() {
        if (!enabled.get()) return false;
        enabled.set(false);
        lastDisabledAt = LocalDateTime.now();
        return true;
    }

    public String getLastEnabledBy() {
        return lastEnabledBy;
    }

    public LocalDateTime getLastEnabledAt() {
        return lastEnabledAt;
    }

    public LocalDateTime getLastDisabledAt() {
        return lastDisabledAt;
    }
}
