package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 403 §Testing / T697–T698：成本预测——分钟桶分桶/滚动清零/速率数值
 * 驱动；projectedOver ≥ 边界；监听缝（含 OVERFLOW）；健康面 details 与
 * UNKNOWN；yml 装配。
 */
class CostForecastTest {

    /** 可拨时钟（分桶确定性）。 */
    private static final class SettableClock extends Clock {
        volatile Instant now = Instant.parse("2026-09-08T00:00:00Z");

        void plusMinutes(long m) {
            now = now.plusSeconds(m * 60);
        }

        @Override
        public Instant instant() {
            return now;
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
    void shouldBucketByMinuteAndRollOldBuckets() {
        SettableClock clock = new SettableClock();
        SpendRateRing ring = new SpendRateRing(60, clock);
        ring.record(100);           // 分钟 0
        clock.plusMinutes(1);
        ring.record(50);            // 分钟 1
        assertThat(ring.windowTotal(Duration.ofHours(1))).isEqualTo(150);

        // 滚动清零：拨到 61 分钟后（环复用分钟 1 的槽），旧桶被覆盖 + 出窗
        clock.plusMinutes(60);
        ring.record(70);
        assertThat(ring.windowTotal(Duration.ofHours(1))).isEqualTo(70); // 0/1 分钟桶均已出窗

        // 速率：窗内 70 microUsd / 60 分钟 → 70/h
        assertThat(ring.ratePerHour(Duration.ofHours(1))).isEqualTo(70);
    }

    @Test
    void shouldProjectOverBudgetAtExactBoundary() {
        SettableClock clock = new SettableClock();
        SpendRateRing ring = new SpendRateRing(60, clock);
        ring.record(500); // 窗 1h：速率 500/h
        // horizon 10h → 外推 5000；预算 5000 → projectedOver（≥ 边界含等）
        CostForecast exact = CostForecast.of(ring, Duration.ofHours(1),
                Duration.ofHours(10), 5000);
        assertThat(exact.ratePerHourMicroUsd()).isEqualTo(500);
        assertThat(exact.horizonMicroUsd()).isEqualTo(5000);
        assertThat(exact.projectedOver()).isTrue();

        CostForecast under = CostForecast.of(ring, Duration.ofHours(1),
                Duration.ofHours(10), 5001);
        assertThat(under.projectedOver()).isFalse();

        // 无预算（≤0）：projectedOver 恒 false（半配置不误报）
        CostForecast noBudget = CostForecast.of(ring, Duration.ofHours(1),
                Duration.ofHours(10), 0);
        assertThat(noBudget.projectedOver()).isFalse();
    }

    @Test
    void shouldFeedRingFromLedgerListenerIncludingOverflow() {
        ModelCostLedger ledger = ModelCostLedger.create();
        List<ModelCostLedger.ModelCost> seen = new CopyOnWriteArrayList<>();
        java.util.function.Consumer<ModelCostLedger.ModelCost> listener = seen::add;
        ledger.addListener(listener);
        ledger.record("m1", 10);
        ledger.record("m1", 0); // 零成本也是事实（record javadoc 口径）
        // OVERFLOW 路径：m1 已占 1 位，再灌 63 个模型至 64 封顶后新模型折入
        for (int i = 0; i < ModelCostLedger.MAX_MODELS - 1; i++) {
            ledger.record("model-" + i, 1);
        }
        ledger.record("model-overflowed", 5);
        assertThat(seen).extracting(ModelCostLedger.ModelCost::model)
                .contains("m1", "m1", ModelCostLedger.OVERFLOW);
        assertThat(seen.stream().filter(c -> c.model().equals(ModelCostLedger.OVERFLOW))
                .mapToLong(ModelCostLedger.ModelCost::microUsd).sum()).isEqualTo(5);

        // 注销幂等（同一引用——方法引用每次新建实例 remove 不掉）
        ledger.removeListener(listener);
        ledger.removeListener(listener);
        int before = seen.size();
        ledger.record("m1", 7);
        assertThat(seen).hasSize(before);
    }

    @Test
    void shouldExposeDetailsAndUnknownWithoutBudget() {
        SettableClock clock = new SettableClock();
        SpendRateRing ring = new SpendRateRing(60, clock);
        ring.record(300);
        io.github.chyuan_cuihongyuan.buzhou.core.health.CostForecastHealth health =
                new io.github.chyuan_cuihongyuan.buzhou.core.health.CostForecastHealth(
                        ring, Duration.ofHours(1), Duration.ofHours(24), 6000);
        assertThat(health.mechanism()).isEqualTo("cost-forecast");
        assertThat(health.status()).isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.UP);
        assertThat(health.details())
                .containsEntry("window-spend-micro-usd", 300L)
                .containsEntry("rate-per-hour-micro-usd", 300L)
                .containsEntry("horizon-projected-micro-usd", 300L * 24)
                .containsEntry("projected-over", true); // 7200 ≥ 6000

        io.github.chyuan_cuihongyuan.buzhou.core.health.CostForecastHealth unbudgeted =
                new io.github.chyuan_cuihongyuan.buzhou.core.health.CostForecastHealth(
                        ring, Duration.ofHours(1), Duration.ofHours(24), 0);
        assertThat(unbudgeted.status())
                .isEqualTo(io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth.Status.UNKNOWN);
        assertThat(unbudgeted.details()).containsEntry("reason", "budget-not-configured");
    }

    @Test
    void shouldAssembleFromYml_whenEnabled() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.budget.forecast.enabled=true",
                        "buzhou.budget.forecast.budget-micro-usd=500000",
                        "buzhou.budget.forecast.horizon=12h")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouCostForecastHealth");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouCostForecastHealth");
                });
    }
}
