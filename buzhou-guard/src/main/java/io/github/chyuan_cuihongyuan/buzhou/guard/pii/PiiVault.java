package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 可逆 PII 代管库（spec 507 / T765，Presidio Vault anonymize↔deanonymize
 * 思想）：vaultize 出稳定令牌占位、服务端留原值，授权场景 restore 还原
 * ——「展示层脱敏、服务端留原值」的可逆工作流原语。
 *
 * <p>令牌 = sha256(original|salt) 前 16 hex：同原值同令牌（存储天然去重）；
 * salt 构造注入防离线字典反查。TTL 惰性过期（Clock 注入）+ maxEntries
 * 有界（超限逐出最旧创建者）；ConcurrentHashMap 线程安全。
 *
 * <p>诚实边界：代管库本身是敏感面——进程内、TTL、有界；hook 自动接线
 * 留扩散（宿主显式控制 vaultize/restore 时点）。
 */
public final class PiiVault {

    /** 令牌占位形态（与不可逆 [PII:TYPE] 族可视觉区分）。 */
    public static final String TOKEN_PREFIX = "[PII-VAULT:";
    public static final String TOKEN_SUFFIX = "]";

    private final String salt;
    private final Duration ttl;
    private final int maxEntries;
    private final Clock clock;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicLong vaulted = new AtomicLong();
    private final AtomicLong restored = new AtomicLong();
    private final AtomicLong unknown = new AtomicLong();

    private record Entry(String original, Instant createdAt) {
    }

    public PiiVault(String salt, Duration ttl, int maxEntries) {
        this(salt, ttl, maxEntries, Clock.systemUTC());
    }

    public PiiVault(String salt, Duration ttl, int maxEntries, Clock clock) {
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("pii vault salt 非空（防离线字典反查）");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("pii vault ttl 须为正时长");
        }
        if (maxEntries < 1) {
            throw new IllegalArgumentException("pii vault max-entries >= 1");
        }
        this.salt = salt;
        this.ttl = ttl;
        this.maxEntries = maxEntries;
        this.clock = clock;
    }

    /** 代管原值，返回稳定占位令牌（同原值同令牌——存储去重）。 */
    public String vaultize(String original) {
        if (original == null || original.isEmpty()) {
            throw new IllegalArgumentException("pii vault 代管原值非空");
        }
        evictExpired();
        if (entries.size() >= maxEntries) {
            entries.entrySet().stream()
                    .min(Map.Entry.comparingByValue((a, b) -> a.createdAt().compareTo(b.createdAt())))
                    .ifPresent(oldest -> entries.remove(oldest.getKey()));
        }
        String token = tokenOf(original);
        entries.putIfAbsent(token, new Entry(original, clock.instant()));
        vaulted.incrementAndGet();
        return TOKEN_PREFIX + token + TOKEN_SUFFIX;
    }

    /** 还原文本内全部有效令牌为原值；过期/未知令牌原样保留（fail-safe）。 */
    public String restore(String text) {
        if (text == null || !text.contains(TOKEN_PREFIX)) {
            return text;
        }
        evictExpired();
        StringBuilder out = new StringBuilder(text.length());
        int cursor = 0;
        while (true) {
            int start = text.indexOf(TOKEN_PREFIX, cursor);
            if (start < 0) {
                out.append(text, cursor, text.length());
                break;
            }
            int end = text.indexOf(TOKEN_SUFFIX, start + TOKEN_PREFIX.length());
            if (end < 0) {
                out.append(text, cursor, text.length());
                break;
            }
            String token = text.substring(start + TOKEN_PREFIX.length(), end);
            Entry entry = entries.get(token);
            if (entry == null) {
                unknown.incrementAndGet();
                out.append(text, cursor, end + TOKEN_SUFFIX.length());
            } else {
                out.append(text, cursor, start).append(entry.original());
                restored.incrementAndGet();
            }
            cursor = end + TOKEN_SUFFIX.length();
        }
        return out.toString();
    }

    /** 已代管条目数（观测）。 */
    public int size() {
        return entries.size();
    }

    public long vaultedCount() {
        return vaulted.get();
    }

    public long restoredCount() {
        return restored.get();
    }

    public long unknownTokenCount() {
        return unknown.get();
    }

    private void evictExpired() {
        Instant now = clock.instant();
        entries.entrySet().removeIf(e -> now.isAfter(e.getValue().createdAt().plus(ttl)));
    }

    private String tokenOf(String original) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + "|" + original).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
