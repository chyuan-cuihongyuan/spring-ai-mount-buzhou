package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 740 / T1082–T1083：供应商限流信号 stats 接线——NaN 起始/更新读数/
 * details 条件出现。
 */
class ResilienceStatsProviderUtilizationTest {

    @Test
    void providerUtilizationStartsNaNAndUpdates() {
        ResilienceStats stats = new ResilienceStats();
        assertThat(Double.isNaN(stats.lastProviderUtilization())).isTrue();
        assertThat(stats.details()).doesNotContainKey("providerUtilization"); // 无信号不出现

        stats.updateProviderUtilization(0.8);
        assertThat(stats.lastProviderUtilization()).isEqualTo(0.8);
        assertThat(stats.details()).containsEntry("providerUtilization", 0.8);

        stats.updateProviderUtilization(0.1); // 回落更新
        assertThat(stats.lastProviderUtilization()).isEqualTo(0.1);
        assertThat(stats.details().get("providerUtilization")).isEqualTo(0.1);
    }
}
