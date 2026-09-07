package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 342 / impl-365：维护 cordon 回归——多源地板 max/入窗抬出窗落/
 * 计数一次/运行时按钮/过期窗 no-op/与冻结并存。
 */
class MaintenanceCordonTest {

    private static final Instant T0 = Instant.parse("2026-09-04T12:00:00Z");

    /** 可推进伪时钟。 */
    private static final class FakeClock extends Clock {
        volatile Instant now = T0;

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
    void floorMergesSourcesBySemanticHighest() {
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        assertThat(floor.get()).isEqualTo(SpawnPriority.LOW); // 无源默认
        floor.set(SpawnPriority.HIGH); // 335 兼容 default 源
        assertThat(floor.get()).isEqualTo(SpawnPriority.HIGH);
        floor.set("maintenance", SpawnPriority.HIGH);
        floor.set(SpawnPriority.LOW); // default 源落回
        assertThat(floor.get()).isEqualTo(SpawnPriority.HIGH); // maintenance 仍生效——正交
        floor.set("maintenance", null);
        assertThat(floor.get()).isEqualTo(SpawnPriority.LOW);
        assertThat(floor.view()).containsKey(SpawnAdmissionFloor.DEFAULT_SOURCE);
    }

    @Test
    void declaredWindowCordonsInside_uncordonsAfter() {
        FakeClock clock = new FakeClock();
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        List<String> counters = new CopyOnWriteArrayList<>();
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.install(
                new io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics() {
                    @Override
                    public void counter(String name, long delta, String... tagKeyValue) {
                        counters.add(name);
                    }

                    @Override
                    public void timer(String name, Duration duration, String... tagKeyValue) {
                    }
                });
        try {
            MaintenanceCordon cordon = new MaintenanceCordon(floor,
                    T0.plusSeconds(600), T0.plusSeconds(3600), "升级窗口",
                    Duration.ofSeconds(15), clock);
            cordon.evaluate(); // 窗前——不 cordon
            assertThat(cordon.cordoned()).isFalse();
            assertThat(floor.get()).isEqualTo(SpawnPriority.LOW);

            clock.now = T0.plusSeconds(1200); // 窗内
            cordon.evaluate();
            cordon.evaluate(); // 二评不重复计数
            assertThat(cordon.cordoned()).isTrue();
            assertThat(floor.get()).isEqualTo(SpawnPriority.HIGH);
            assertThat(counters.stream().filter("buzhou.maintenance.cordoned"::equals).count())
                    .isEqualTo(1);

            clock.now = T0.plusSeconds(4000); // 窗出
            cordon.evaluate();
            assertThat(cordon.cordoned()).isFalse();
            assertThat(floor.get()).isEqualTo(SpawnPriority.LOW);
            assertThat(counters).contains("buzhou.maintenance.uncordoned");
        } finally {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.reset();
        }
    }

    @Test
    void runtimeButtonsImmediate_effectiveUntilManualUncordon() {
        FakeClock clock = new FakeClock();
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        MaintenanceCordon cordon = new MaintenanceCordon(floor,
                null, null, "", Duration.ofSeconds(15), clock);
        assertThat(cordon.cordoned()).isFalse(); // 无窗零行为

        cordon.cordon("事故临时停进");
        assertThat(cordon.cordoned()).isTrue(); // 即时生效
        assertThat(floor.get()).isEqualTo(SpawnPriority.HIGH);
        clock.now = T0.plusSeconds(86_400); // 一天后仍 cordon（手动解除前持续）
        cordon.evaluate();
        assertThat(cordon.cordoned()).isTrue();

        cordon.uncordon();
        assertThat(cordon.cordoned()).isFalse();
        assertThat(floor.get()).isEqualTo(SpawnPriority.LOW);
    }

    @Test
    void expiredWindowAtStartupIsNoOp() {
        FakeClock clock = new FakeClock();
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        MaintenanceCordon cordon = new MaintenanceCordon(floor,
                T0.minusSeconds(7200), T0.minusSeconds(3600), "过期窗",
                Duration.ofSeconds(15), clock);
        cordon.evaluate();
        cordon.evaluate();
        assertThat(cordon.cordoned()).isFalse(); // 不追溯
        assertThat(floor.get()).isEqualTo(SpawnPriority.LOW);
    }

    @Test
    void coexistsWithErrorBudgetFreeze_cordonReleaseKeepsFreeze() {
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        floor.set(SpawnPriority.HIGH); // 335 冻结抬 default 源
        FakeClock clock = new FakeClock();
        MaintenanceCordon cordon = new MaintenanceCordon(floor,
                T0.plusSeconds(600), T0.plusSeconds(3600), "升级",
                Duration.ofSeconds(15), clock);
        clock.now = T0.plusSeconds(1200);
        cordon.evaluate();
        assertThat(floor.get()).isEqualTo(SpawnPriority.HIGH);
        clock.now = T0.plusSeconds(4000);
        cordon.evaluate(); // cordon 窗出落回
        assertThat(floor.get()).isEqualTo(SpawnPriority.HIGH); // 冻结仍在——正交不互踩
    }

    @Test
    void stopReleasesOwnFloorSource() {
        FakeClock clock = new FakeClock();
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        MaintenanceCordon cordon = new MaintenanceCordon(floor,
                null, null, "", Duration.ofSeconds(15), clock);
        cordon.start();
        try {
            cordon.cordon("停机前");
            assertThat(floor.get()).isEqualTo(SpawnPriority.HIGH);
        } finally {
            cordon.stop();
        }
        assertThat(floor.get()).isEqualTo(SpawnPriority.LOW); // 离场不留抬着的地板
        assertThatThrownBy(() -> new MaintenanceCordon(floor,
                T0.plusSeconds(3600), T0.plusSeconds(600), "", Duration.ofSeconds(15), clock))
                .isInstanceOf(IllegalArgumentException.class); // from ≥ until 红
    }
}
