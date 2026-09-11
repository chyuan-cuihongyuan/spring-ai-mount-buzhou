package io.github.chyuan_cuihongyuan.buzhou.guard.leak;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 跨会话泄漏金丝雀注册表（spec 528 / T809，thinkst canarytokens/honeytoken
 * 思想）：每会话种植专属令牌（确定性 sha256(sessionId|salt) 前 8 hex），
 * 扫描任意文本中**属于其他会话**的令牌即泄漏信号——跨会话污染（共享缓存
 * 串话/历史泄漏）的探测面。与 CanaryGuardHook（注入检测）语义正交。
 *
 * <p>诚实边界：检测依赖令牌原样出现（模型改写/截断不保——概率探针非隔离
 * 机制）；注册表 LRU 256 有界（tag 有界纪律）。
 */
public final class SessionCanaryRegistry {

    /** 令牌前缀（扫描锚点）。 */
    public static final String TOKEN_PREFIX = "BUZHOU-LEAKCANARY-";
    /** 默认注册表容量（LRU）。 */
    public static final int DEFAULT_CAPACITY = 256;

    /** 泄漏命中（from = 令牌主会话——泄漏来源）。 */
    public record LeakFrom(String leakedFromSession, String token) {
    }

    private final String salt;
    private final int capacity;
    /** sessionId → token（LRU：accessOrder——capacity 守卫）。 */
    private final Map<String, String> sessions = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > capacity;
        }
    };
    private final Map<String, String> tokenToSession = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong detected = new java.util.concurrent.atomic.AtomicLong();

    public SessionCanaryRegistry(String salt) {
        this(salt, DEFAULT_CAPACITY);
    }

    public SessionCanaryRegistry(String salt, int capacity) {
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("金丝雀 salt 非空（防离线推演）");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity >= 1");
        }
        this.salt = salt;
        this.capacity = capacity;
    }

    /** 种植（或复取）会话专属令牌——确定性：同会话恒同令牌。 */
    public synchronized String plant(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("sessionId 非空");
        }
        String existing = sessions.get(sessionId);
        if (existing != null) {
            return existing;
        }
        String token = tokenOf(sessionId);
        sessions.put(sessionId, token);
        tokenToSession.put(token, sessionId);
        return token;
    }

    /**
     * 扫描文本：返回其中出现的、属于**其他会话**的令牌泄漏列表（观察者
     * 自己的令牌不算泄漏——正常回显）；无命中空列表。
     */
    public List<LeakFrom> detect(String observerSessionId, String text) {
        List<LeakFrom> leaks = new ArrayList<>();
        if (text == null || !text.contains(TOKEN_PREFIX)) {
            return leaks;
        }
        int idx = 0;
        while ((idx = text.indexOf(TOKEN_PREFIX, idx)) >= 0) {
            int start = idx + TOKEN_PREFIX.length();
            int end = start;
            while (end < text.length() && isHex(text.charAt(end)) && end - start < 8) {
                end++;
            }
            if (end - start == 8) {
                String token = text.substring(idx, end);
                String owner = tokenToSession.get(token);
                if (owner != null && !owner.equals(observerSessionId)) {
                    leaks.add(new LeakFrom(owner, token));
                    BuzhouMetricsHolder.metrics().counter("buzhou.guard.leak.detected");
                }
            }
            idx = start;
        }
        if (!leaks.isEmpty()) {
            detected.addAndGet(leaks.size());
        }
        return leaks;
    }

    /** 已种植会话数（观测）。 */
    public synchronized int size() {
        return sessions.size();
    }

    public long detectedCount() {
        return detected.get();
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }

    private String tokenOf(String sessionId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    (salt + "|" + sessionId).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(8);
            for (int i = 0; i < 4; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return TOKEN_PREFIX + hex;
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
