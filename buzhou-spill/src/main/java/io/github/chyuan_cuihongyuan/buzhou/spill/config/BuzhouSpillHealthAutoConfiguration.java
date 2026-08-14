package io.github.chyuan_cuihongyuan.buzhou.spill.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

/**
 * spill 健康装配（impl-41 / spec 13 §T66）：独立于模块开关——禁用报 UNKNOWN。
 * rootDir 取 {@code buzhou.spill.root-dir}（与 SpillProperties 同源；禁用时探针不执行）。
 */
@AutoConfiguration
public class BuzhouSpillHealthAutoConfiguration {

    @Bean
    public SpillHealth spillHealth(org.springframework.core.env.Environment env) {
        boolean enabled = env.getProperty("buzhou.spill.enabled", Boolean.class, true);
        // 与 SpillProperties 同源默认（impl-42 迁移）：独立临时目录
        String configured = env.getProperty("buzhou.spill.root-dir", String.class,
                System.getProperty("java.io.tmpdir") + "/buzhou-spill");
        return new SpillHealth(enabled, Path.of(configured));
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
    static class SpillHealthIndicatorConfiguration {

        @Bean
        public BuzhouHealthIndicator spillHealthIndicator(SpillHealth delegate) {
            return new BuzhouHealthIndicator(delegate);
        }
    }
}
