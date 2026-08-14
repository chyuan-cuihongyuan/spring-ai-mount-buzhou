package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import java.time.Duration;
import java.util.Map;

/**
 * guard 策略门配置（impl-40 / spec 13 §T64）：{@code buzhou.guard.policy.*} 子树解析。
 *
 * <pre>
 * buzhou.guard.policy.enabled=false                       # 默认关（默认拒引擎需显式启用）
 * buzhou.guard.policy.source=classpath:buzhou-policy.json # classpath: / file: / 裸路径
 * buzhou.guard.policy.refresh-interval=PT30S              # 0 = 关闭轮询（仅启动加载）
 * </pre>
 *
 * @param enabled         策略门开关（默认 false——deny-by-default 引擎不静默上线）
 * @param sourceLocation  规则来源（classpath:/file:/裸路径）
 * @param refreshInterval 轮询间隔（null/0/负 = 关闭）
 */
public record GuardPolicyConfig(boolean enabled, String sourceLocation, Duration refreshInterval) {

    public static final String DEFAULT_SOURCE = "classpath:buzhou-policy.json";
    public static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofSeconds(30);

    public static GuardPolicyConfig defaults() {
        return new GuardPolicyConfig(false, DEFAULT_SOURCE, DEFAULT_REFRESH_INTERVAL);
    }

    /** {@code buzhou.guard} 子树中的 {@code policy} 子 Map 解析（缺失返回默认）。 */
    public static GuardPolicyConfig fromGuardMap(Map<String, Object> guardMap) {
        if (guardMap == null || !(guardMap.get("policy") instanceof Map<?, ?> raw)) {
            return defaults();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> policy = (Map<String, Object>) raw;
        boolean enabled = policy.get("enabled") instanceof Boolean b && b;
        String source = policy.get("source") instanceof String s && !s.isBlank()
                ? s.trim() : DEFAULT_SOURCE;
        Duration interval = parseDuration(policy.get("refresh-interval"),
                DEFAULT_REFRESH_INTERVAL);
        return new GuardPolicyConfig(enabled, source, interval);
    }

    private static Duration parseDuration(Object value, Duration fallback) {
        if (value instanceof Duration d) {
            return d;
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Duration.parse(text.trim());
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
