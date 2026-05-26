package com.example.study.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

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
