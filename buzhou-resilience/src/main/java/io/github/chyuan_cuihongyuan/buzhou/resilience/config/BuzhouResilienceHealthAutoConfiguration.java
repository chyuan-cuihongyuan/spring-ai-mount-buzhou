package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealthIndicator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 韧性层健康装配（impl-44 / spec 14 §A，对齐 guard 的 GuardHealth 范式）：
 * 独立于模块开关——禁用时报 UNKNOWN（带 disabled 详情）；启用 = UP + 计数快照
 * （重试 / 耗尽 / 限流拒绝 / 超时 / 最近错误分类）。纯内存机制无 DOWN 态。
 */
@AutoConfiguration
public class BuzhouResilienceHealthAutoConfiguration {

    @Bean
    public ResilienceHealth resilienceHealth(Environment env, ObjectProvider<ResilienceStats> stats) {
        boolean enabled = env.getProperty("buzhou.resilience.enabled", Boolean.class, true);
        return new ResilienceHealth(enabled, stats.getIfAvailable());
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
    static class ResilienceHealthIndicatorConfiguration {

        @Bean
        public BuzhouHealthIndicator resilienceHealthIndicator(ResilienceHealth delegate) {
            return new BuzhouHealthIndicator(delegate);
        }
    }

    /** 健康委托：禁用 UNKNOWN；启用 UP + ResilienceStats 快照（无 stats 时仅报 enabled）。 */
    public static final class ResilienceHealth implements io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth {

        private final boolean enabled;
        private final ResilienceStats stats;

        ResilienceHealth(boolean enabled, ResilienceStats stats) {
            this.enabled = enabled;
            this.stats = stats;
        }

        @Override
        public String mechanism() {
            return "resilience";
        }

        @Override
        public Status status() {
            return enabled ? Status.UP : Status.UNKNOWN;
        }

        @Override
        public java.util.Map<String, Object> details() {
            if (!enabled) {
                return java.util.Map.of("enabled", false);
            }
            return stats != null ? stats.details() : java.util.Map.of("enabled", true);
        }
    }
}
