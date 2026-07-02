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

    public boolean isEnabled() {
        return enabled.get();
    }

    public synchronized boolean enable(String byUsername) {
        if (enabled.get()) return false;
        enabled.set(true);
        lastEnabledBy = byUsername;
        lastEnabledAt = LocalDateTime.now();
        return true;
    }

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
