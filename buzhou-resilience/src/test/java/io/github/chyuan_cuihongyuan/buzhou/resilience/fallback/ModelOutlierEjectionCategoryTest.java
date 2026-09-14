package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 离群驱逐分类感知测试（spec 1610 / T2371–T2372 / impl 1163）：分类 ∈
 * failureCategories 才计连错（熔断 failure-categories 同口径——AUTH/CONTENT 类
 * 驱赶端点无意义）；自定义分类集；成功复位。
 */
class ModelOutlierEjectionCategoryTest {

    static final class MutableClock extends Clock {
        private volatile Instant instant = Instant.parse("2026-09-15T00:00:00Z");

        void advance(Duration d) {
            instant = instant.plus(d);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @Test
    void onlyDrivingCategoriesCountTowardEjection() {
        MutableClock clock = new MutableClock();
        ModelOutlierEjection ejection = new ModelOutlierEjection(
                new ModelOutlierEjection.Config(3, Duration.ofMinutes(1)), clock);
        // AUTH（配置错误）与 CONTENT（内容治理）不驱动
        ejection.recordError("m", "AUTH");
        ejection.recordError("m", "CONTENT");
        ejection.recordError("m", "RATE_LIMIT");
        assertThat(ejection.isEjected("m")).isFalse();
        // 三次 NETWORK（∈ 默认驱动集）即驱逐
        ejection.recordError("m", "NETWORK");
        ejection.recordError("m", "SERVER");
        ejection.recordError("m", "TIMEOUT");
        assertThat(ejection.isEjected("m")).isTrue();
        // 驱逐窗口过后复池
        clock.advance(Duration.ofMinutes(2));
        assertThat(ejection.isEjected("m")).isFalse();
    }

    @Test
    void customCategoriesAreHonored() {
        ModelOutlierEjection ejection = new ModelOutlierEjection(
                new ModelOutlierEjection.Config(1, Duration.ofMinutes(1), 0, Set.of("SERVER")), null);
        ejection.recordError("m", "NETWORK"); // 默认集成员但自定义集外——不计
        assertThat(ejection.isEjected("m")).isFalse();
        ejection.recordError("m", "server"); // 大小写归一
        assertThat(ejection.isEjected("m")).isTrue();
    }

    @Test
    void successResetsConsecutiveErrors() {
        ModelOutlierEjection ejection = new ModelOutlierEjection(
                new ModelOutlierEjection.Config(2, Duration.ofMinutes(1)), null);
        ejection.recordError("m", "NETWORK");
        ejection.recordSuccess("m"); // 健康调用抵销劣化轨迹
        ejection.recordError("m", "NETWORK");
        assertThat(ejection.isEjected("m")).isFalse(); // 连错重新从 1 起
    }

    @Test
    void configConversionFromPropertiesGroup() {
        io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties.Outlier group =
                new io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties.Outlier(
                        Boolean.TRUE, 7, Duration.ofMinutes(5), 50,
                        java.util.List.of("NETWORK", "SERVER"));
        assertThat(group.effectiveEnabled()).isTrue();
        ModelOutlierEjection.Config config = group.toEjectionConfig();
        assertThat(config.consecutiveErrors()).isEqualTo(7);
        assertThat(config.ejectionWindow()).isEqualTo(Duration.ofMinutes(5));
        assertThat(config.panicThresholdPercent()).isEqualTo(50);
        assertThat(config.failureCategories()).containsExactlyInAnyOrder("NETWORK", "SERVER");
    }
}
