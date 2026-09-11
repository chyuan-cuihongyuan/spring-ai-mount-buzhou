package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

/**
 * Webhook 验签与防重放（spec 428 / T747，Stripe signed webhooks 借鉴）：
 * 消费端工具——与 {@link WebhookEventForwarder} 同 crypto 路的 HMAC-SHA256
 * 常量时间验签 + 时间戳容差窗（重放窗口有界）。
 *
 * <p>fail-closed：null/空/畸形输入一律 {@code false} 不抛（HTTP 响应语义
 * 归消费端——库只裁真伪）。时间戳头由 forwarder 随签名加发
 * {@code X-Buzhou-Timestamp: epochSeconds}（不进 MAC——存量验签消费端
 * 零破坏；新消费端用容差窗重载防重放）。
 */
public final class WebhookSignatures {

    /** 建议容差窗（Stripe 同款 5 分钟）。 */
    public static final Duration DEFAULT_TOLERANCE = Duration.ofMinutes(5);

    private WebhookSignatures() {
    }

    /** 产出签名（与 forwarder 同实现——签名/验签两侧永不漂移）。 */
    public static String sign(String secret, String body) {
        return WebhookEventForwarder.hmacSha256(secret, body);
    }

    /** 常量时间验签（任何缺失/畸形 false——fail-closed）。 */
    public static boolean verify(String secret, String body, String hexSignature) {
        if (secret == null || secret.isBlank() || body == null || hexSignature == null) {
            return false;
        }
        byte[] expected = sign(secret, body).getBytes(StandardCharsets.UTF_8);
        byte[] provided = hexSignature.getBytes(StandardCharsets.UTF_8);
        // 常量时间比对——防时序侧信道（长度不同立即 false 是 isEqual 既有语义）
        return MessageDigest.isEqual(expected, provided);
    }

    /**
     * spec 540 / T833：双密钥轮换验签——先 current 后 previous（轮换窗口内
     * 旧签名仍可验），两者皆不匹配 false（fail-closed）。previous 可 null
     * （单密钥期）。
     */
    public static boolean verifyWithRotation(String currentSecret, String previousSecret,
            String body, String hexSignature) {
        if (verify(currentSecret, body, hexSignature)) {
            return true;
        }
        return previousSecret != null && !previousSecret.isBlank()
                && verify(previousSecret, body, hexSignature);
    }

    /**
     * spec 540：轮换验签 + 时间戳容差窗（轮换 × 重放窗组合——轮换窗口内旧
     * 密钥签名仍可验但重放窗依旧有界）。
     */
    public static boolean verifyWithRotation(String currentSecret, String previousSecret,
            String body, String hexSignature, String timestampEpochSeconds, Duration tolerance) {
        if (verify(currentSecret, body, hexSignature)) {
            return verifyTimestamp(timestampEpochSeconds, tolerance);
        }
        if (previousSecret == null || previousSecret.isBlank()
                || !verify(previousSecret, body, hexSignature)) {
            return false;
        }
        return verifyTimestamp(timestampEpochSeconds, tolerance);
    }

    private static boolean verifyTimestamp(String timestampEpochSeconds, Duration tolerance) {
        if (timestampEpochSeconds == null || timestampEpochSeconds.isBlank()) {
            return false;
        }
        long ts;
        try {
            ts = Long.parseLong(timestampEpochSeconds.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000;
        return Math.abs(now - ts) <= tolerance.toSeconds();
    }

    /**
     * 验签 + 时间戳容差窗（重放窗口有界）：{@code |now - ts| <= tolerance}
     * 才过；时间戳缺失/非数字 → false（fail-closed）。
     *（重放窗口有界）：{@code |now - ts| <= tolerance}
     * 才过；时间戳缺失/非数字 → false（fail-closed）。
     *
     * @param timestampHeader forwarder 加发的 {@code X-Buzhou-Timestamp}（epoch 秒）
     */
    public static boolean verify(String secret, String body, String hexSignature,
            String timestampHeader, Duration tolerance, Instant now) {
        if (!verify(secret, body, hexSignature)) {
            return false;
        }
        if (timestampHeader == null || tolerance == null || tolerance.isNegative()
                || tolerance.isZero() || now == null) {
            return false;
        }
        long ts;
        try {
            ts = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        Instant signedAt = Instant.ofEpochSecond(ts);
        Duration drift = Duration.between(signedAt, now).abs();
        return drift.compareTo(tolerance) <= 0;
    }
}
