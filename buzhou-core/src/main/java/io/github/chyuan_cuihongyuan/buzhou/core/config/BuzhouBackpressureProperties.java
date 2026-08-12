package io.github.chyuan_cuihongyuan.buzhou.core.config;

import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 背压与多层限流装配属性（spec「背压与多层限流」，前缀 {@code buzhou.backpressure}）。
 *
 * <p>三维挂点：① spawn 并发会话上限 ② 每会话工具扇出上限 ③ 模型 RPM+TPM 双桶（后者归
 * {@code buzhou.resilience.rate-limit} 前缀，见 {@code ResilienceProperties}）。
 *
 * <p>字段统一用 boxed 类型，null = 「未配置 / 不限」（对齐 {@code BuzhouShutdownProperties} 模板）。
 * <b>safe-by-default</b>：阈值默认 null = 不限，显式配置才生效——不设可能误伤生产的魔法默认值。
 * 保留的现值（每轮 8 / 工具超时 60s）在装配层抽命名常量，禁魔法数字。
 *
 * @param enabled              机制总开关（默认开，safe-by-default；关则回退底座原生行为——不限并发、不限扇出）
 * @param maxConcurrentSessions 实例级并发活跃会话上限（null = 不限）；超限时按 {@code spawnOverloadPolicy} 处置
 * @param spawnQueueTimeout    spawn 排队等待超时（QUEUE 档生效；null = 取保守默认 30s）
 * @param spawnOverloadPolicy  spawn 过载策略（null = QUEUE；FAIL_FAST = 不排队直接拒）
 * @param tool                 工具扇出闸参数组（每轮并发上限 / 工具超时 / 许可获取超时 / 过载策略）
 */
@ConfigurationProperties(prefix = "buzhou.backpressure")
public record BuzhouBackpressureProperties(
        Boolean enabled,
        Integer maxConcurrentSessions,
        Duration spawnQueueTimeout,
        String spawnOverloadPolicy,
        Tool tool) {

    /** spawn 排队超时保守默认（未配置时兜底）。 */
    public static final Duration DEFAULT_SPAWN_QUEUE_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 工具扇出闸参数组。
     *
     * @param maxConcurrentPerTurn 每轮工具并发上限（null = 取现值常量 8）
     * @param toolTimeout          单工具执行超时（null = 取现值常量 60s）
     * @param permitAcquireTimeout 扇出许可获取超时（null = 无限等待，保持现状；配置后改为有界 tryAcquire）
     * @param overloadPolicy       工具过载策略（null = QUEUE；FAIL_FAST 等价 permitAcquireTimeout=0）
     */
    public record Tool(
            Integer maxConcurrentPerTurn,
            Duration toolTimeout,
            Duration permitAcquireTimeout,
            String overloadPolicy) {
    }

    public BuzhouBackpressureProperties {
        enabled = enabled == null || enabled;
        // 阈值字段保持 null = 未配置，由装配层派生
    }

    /** 全默认（装配测试 / 兜底用）。 */
    public static BuzhouBackpressureProperties defaults() {
        return new BuzhouBackpressureProperties(null, null, null, null, null);
    }

    /** spawn 过载策略生效值（null / 非法值落回 QUEUE 默认档）。 */
    public OverloadPolicy effectiveSpawnOverloadPolicy() {
        return parsePolicy(spawnOverloadPolicy);
    }

    /** 工具过载策略生效值（null / 非法值落回 QUEUE 默认档）。 */
    public OverloadPolicy effectiveToolOverloadPolicy() {
        return parsePolicy(tool == null ? null : tool.overloadPolicy());
    }

    /** spawn 排队超时生效值（null 取保守默认）。 */
    public Duration effectiveSpawnQueueTimeout() {
        return spawnQueueTimeout != null ? spawnQueueTimeout : DEFAULT_SPAWN_QUEUE_TIMEOUT;
    }

    private static OverloadPolicy parsePolicy(String value) {
        if (value == null || value.isBlank()) {
            return OverloadPolicy.QUEUE;
        }
        try {
            return OverloadPolicy.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return OverloadPolicy.QUEUE;
        }
    }
}
