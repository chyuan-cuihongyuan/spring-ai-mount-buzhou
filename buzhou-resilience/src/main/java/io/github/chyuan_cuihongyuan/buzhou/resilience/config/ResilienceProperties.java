package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * 模型韧性层装配属性（spec「模型韧性层 M1」/ 09 / ticket 22，前缀 {@code buzhou.resilience}）。
 *
 * <p>字段统一用 boxed 类型，null = 「未配置」→ 取规范默认（对齐 {@code SpillProperties} 模板），
 * 这样 yml 只覆盖个别参数时其余仍取默认，而非被零值抹掉。
 *
 * @param enabled             整个韧性机制开关（默认开，safe-by-default；关则回退底座原生行为）
 * @param maxAttempts         最大尝试次数（含首次；默认 3，即首调 + 最多 2 次重试）
 * @param initialBackoff      首次重试退避基数（默认 500ms）
 * @param maxBackoff          退避上限（默认 10s；Retry-After 与指数增长都钳制到本值）
 * @param multiplier          指数退避乘子（默认 2.0；1.0 = 恒定退避）
 * @param jitter              抖动因子 {@code [0,1]}（默认 0.5；多实例同时重试时打散惊群，0 = 关闭抖动）
 * @param retryableCategories 可重试类别集合（大小写无关；默认 {@code [RATE_LIMIT, NETWORK]}，
 *                            覆盖默认可重试表，如把 {@code UNKNOWN} 配为重试）
 * @param deadline            模型调用统一超时（默认 60s；超时取消在途调用并作为终态失败。
 *                            显式设为 0 关闭超时——不推荐生产关闭）
 */
@ConfigurationProperties(prefix = "buzhou.resilience")
public record ResilienceProperties(
        Boolean enabled,
        Integer maxAttempts,
        Duration initialBackoff,
        Duration maxBackoff,
        Double multiplier,
        Double jitter,
        List<String> retryableCategories,
        Duration deadline,
        RateLimit rateLimit) {

    /**
     * 模型 RPM+TPM 双桶限流参数组（spec「背压 · 维度③」）。
     *
     * <p>前缀 {@code buzhou.resilience.rate-limit}。boxed null = 不限（safe-by-default）。
     *
     * @param requestsPerMinute 每分钟请求数上限（null = 不限）
     * @param tokensPerMinute   每分钟 token 数上限（null = 不限；TPM 事后记账+下次预检=平均速率保护）
     * @param queueTimeout      QUEUE 档排队超时（null = 取保守默认 30s）
     * @param overloadPolicy    过载策略（null = QUEUE；FAIL_FAST = 不排队直接拒）
     */
    public record RateLimit(
            Integer requestsPerMinute,
            Integer tokensPerMinute,
            Duration queueTimeout,
            String overloadPolicy) {
    }

    public ResilienceProperties {
        enabled = enabled == null ? true : enabled;
        maxAttempts = maxAttempts == null || maxAttempts < 1 ? 3 : maxAttempts;
        initialBackoff = initialBackoff == null || initialBackoff.isNegative() ? Duration.ofMillis(500) : initialBackoff;
        maxBackoff = maxBackoff == null || maxBackoff.isNegative() ? Duration.ofSeconds(10) : maxBackoff;
        multiplier = multiplier == null || multiplier <= 0 ? 2.0 : multiplier;
        jitter = jitter == null || jitter < 0 || jitter > 1 ? 0.5 : jitter;
        retryableCategories = retryableCategories == null || retryableCategories.isEmpty()
                ? List.of("RATE_LIMIT", "NETWORK") : List.copyOf(retryableCategories);
        deadline = deadline == null ? Duration.ofSeconds(60) : deadline;
        // rateLimit 保持 null = 未配置（不限），由模块层判定
    }

    /** 全默认（装配测试 / 兜底用）。 */
    public static ResilienceProperties defaults() {
        return new ResilienceProperties(null, null, null, null, null, null, null, null, null);
    }

    /** rate-limit 过载策略生效值（null / 非法值落回 QUEUE 默认档）。 */
    public io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy effectiveRateLimitOverloadPolicy() {
        if (rateLimit == null || rateLimit.overloadPolicy() == null || rateLimit.overloadPolicy().isBlank()) {
            return io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy.QUEUE;
        }
        try {
            return io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy.valueOf(
                    rateLimit.overloadPolicy().trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy.QUEUE;
        }
    }
}
