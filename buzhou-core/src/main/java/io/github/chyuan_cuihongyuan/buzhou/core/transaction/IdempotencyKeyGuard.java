package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

/**
 * 幂等键判定面（spec 1893 / T2987 / impl 1494）——Stripe
 * idempotency-key 三态语义：同键同参 → REPLAY（重放缓存响应）；
 * 同键异参 → CONFLICT（idempotency_error——把 A 的结果给 B 的副作用
 * 错配被物理拦住）；首见 → FIRST。TTL 到期键失效可回收。
 *
 * <p>纯函数零状态；不做键存储与重放（归 501 IdempotencyAdvisor 面）。
 */
public final class IdempotencyKeyGuard {

    private IdempotencyKeyGuard() {
    }

    /** 判定结果：FIRST 首见放行 / REPLAY 同键同参重放 / CONFLICT 同键异参拒绝。 */
    public enum Decision { FIRST, REPLAY, CONFLICT }

    /**
     * 三态判定：storedFingerprint 为 null = 首见；与 incoming 相等 =
     * REPLAY；不等 = CONFLICT。incoming 为 null 仅在 FIRST 之外的
     * 路径按 CONFLICT 处理（无指纹的复用请求不可信）。
     */
    public static Decision decide(String storedFingerprint,
                                  String incomingFingerprint) {
        if (storedFingerprint == null) {
            return Decision.FIRST;
        }
        if (incomingFingerprint != null && incomingFingerprint.equals(storedFingerprint)) {
            return Decision.REPLAY;
        }
        return Decision.CONFLICT;
    }

    /**
     * TTL 失效判定：now ≥ created + ttl 即失效（边界恰到期即失效）。
     * 契约：ttl ≥ 0、created ≥ 0、now ≥ 0（fail-fast）。
     */
    public static boolean isExpired(long createdAtMillis, long nowMillis,
                                    long ttlMillis) {
        if (ttlMillis < 0) {
            throw new IllegalArgumentException("ttl 不能为负：" + ttlMillis);
        }
        if (createdAtMillis < 0 || nowMillis < 0) {
            throw new IllegalArgumentException(String.format(
                    "时点不能为负：created=%d, now=%d", createdAtMillis, nowMillis));
        }
        return nowMillis >= createdAtMillis + ttlMillis;
    }
}
