package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 熔断时间窗生效读面测试（spec 638 / T926–T927 / impl 491）：
 * stats.details().circuitTimeWindowMs 随配置（0=count 窗缺省 / 90000=声明值）。
 */
class CircuitTimeWindowReadoutTest {

    @Test
    void timeWindowVisibleInStats() {
        ResilienceStats stats = new ResilienceStats();
        ResilienceModule.configure(new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null,
                null, null, null, null, null, null, null), "m", stats,
                null, null, null, null, null);
        assertThat(stats.details()).containsEntry("circuitTimeWindowMs", 0L); // count 窗缺省

        ResilienceStats windowed = new ResilienceStats();
        ResilienceModule.configure(new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null,
                null, new ResilienceProperties.Circuit(null, null, null, null, null, null,
                        null, null, Duration.ofMinutes(10)),
                null, null, null, null, null, null), "m", windowed,
                null, null, null, null, null);
        assertThat(windowed.details()).containsEntry("circuitTimeWindowMs", 600_000L);
    }
}
