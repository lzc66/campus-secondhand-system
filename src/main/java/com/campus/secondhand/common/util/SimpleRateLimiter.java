package com.campus.secondhand.common.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轻量内存限流器(固定窗口计数),用于匿名接口的基础防护:
 * 验证码获取、登录、学生证上传、注册提交等无鉴权接口按 IP 限频。
 * 单实例部署足够;多实例部署时应替换为 Redis 等共享存储。
 */
public final class SimpleRateLimiter {

    private static final Map<String, Window> STORE = new ConcurrentHashMap<>();

    private SimpleRateLimiter() {
    }

    /**
     * @return true 表示允许本次请求;false 表示窗口内次数已用完
     */
    public static boolean tryAcquire(String key, int limit, long windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000L;
        Window window = STORE.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startedAt >= windowMillis) {
                return new Window(now);
            }
            return existing;
        });
        int count = window.counter.incrementAndGet();
        return count <= limit;
    }

    public static void cleanup() {
        long now = System.currentTimeMillis();
        // 惰性清理:仅在表较大时执行,避免每次请求全量扫描
        if (STORE.size() < 10_000) {
            return;
        }
        STORE.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= 30 * 60 * 1000L);
    }

    private static final class Window {
        private final long startedAt;
        private final AtomicInteger counter = new AtomicInteger(0);

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
