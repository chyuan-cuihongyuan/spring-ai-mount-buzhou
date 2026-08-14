package io.github.chyuan_cuihongyuan.buzhou.memory.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealthIndicator;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * memory 健康装配（impl-41 / spec 13 §T66）：<b>独立于模块开关</b>——机制禁用时
 * {@link MemoryHealth} 报 UNKNOWN（缺席不是安静；健康聚合不误 DOWN）。actuator 在场时
 * 追加 {@link BuzhouHealthIndicator} 适配。
 */
@AutoConfiguration
public class BuzhouMemoryHealthAutoConfiguration {

    @Bean
    public MemoryHealth memoryHealth(BuzhouStores stores,
            org.springframework.core.env.Environment env) {
        boolean enabled = env.getProperty("buzhou.memory.enabled", Boolean.class, true);
        return new MemoryHealth(enabled, stores);
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
    static class MemoryHealthIndicatorConfiguration {

        @Bean
        public BuzhouHealthIndicator memoryHealthIndicator(MemoryHealth delegate) {
            return new BuzhouHealthIndicator(delegate);
        }
    }
}
