package io.github.chyuan_cuihongyuan.buzhou.core.policy;

/**
 * API 弃用日落生命周期（spec 1916 / T3033 / impl 1517）——Stripe/
 * GitHub 版本日落语义：三阶段 ACTIVE（正常）→ DEPRECATED（仍可用
 * 但发警告）→ SUNSET（拒绝服务）。弃用公告与移除之间的灰色期有
 * 明确刻度——调用方有倒计时，宿主有删除依据。
 *
 * <p>纯函数零状态；声明存储归 ToolDeprecation 配置面。
 */
public final class ApiSunsetLifecycle {

    private ApiSunsetLifecycle() {
    }

    /** 生命周期三段：ACTIVE → DEPRECATED（警告期）→ SUNSET（拒绝）。 */
    public enum Phase { ACTIVE, DEPRECATED, SUNSET }

    /**
     * 时刻判定：now &lt; deprecatedAt → ACTIVE；&lt; sunsetAt →
     * DEPRECATED；≥ sunsetAt → SUNSET（边界含上）。契约：0 ≤
     * deprecatedAt ≤ sunsetAt、now ≥ 0（fail-fast）。
     */
    public static Phase phase(long deprecatedAt, long sunsetAt, long now) {
        validate(deprecatedAt, sunsetAt, now);
        if (now >= sunsetAt) {
            return Phase.SUNSET;
        }
        if (now >= deprecatedAt) {
            return Phase.DEPRECATED;
        }
        return Phase.ACTIVE;
    }

    /**
     * 剩余时间读数：sunsetAt − now 与 0 取大（已日落钳 0）。
     */
    public static long daysRemainingMillis(long sunsetAt, long now) {
        validate(0, sunsetAt, now);
        return Math.max(0, sunsetAt - now);
    }

    private static void validate(long deprecatedAt, long sunsetAt, long now) {
        if (now < 0) {
            throw new IllegalArgumentException("now 不能为负：" + now);
        }
        if (deprecatedAt < 0 || sunsetAt < deprecatedAt) {
            throw new IllegalArgumentException(String.format(
                    "须满足 0 ≤ deprecatedAt ≤ sunsetAt：%d, %d",
                    deprecatedAt, sunsetAt));
        }
    }
}
