package com.dcg.gateway.model.config.auth;

import lombok.Data;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class TokenCache {
    private final AtomicReference<String> token = new AtomicReference<>();
    private final AtomicReference<Long> expireTime = new AtomicReference<>();
    private final ReentrantLock refreshLock = new ReentrantLock();

    public void update(String newToken, long newExpireTime) {
        token.set(newToken);
        expireTime.set(newExpireTime);
    }

    public boolean needRefresh() {
        Long expire = expireTime.get();
        if (expire == null) return true;
        return System.currentTimeMillis() + 300000 > expire;  // 提前5分钟刷新
    }

    public String getToken() {
        return token.get();
    }
} 