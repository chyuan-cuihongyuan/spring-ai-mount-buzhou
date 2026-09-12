package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 508 / T767–T768：成本尖峰检测——Clock 注入基线满后突刺触发
 * （listener+计数）、样本不足不触发、噪声地板不触发、冷却防抖一次、
 * yml 装配缺席。
 */
class CostSpikeDetectorTest {

    private static Clock mutableClock(AtomicReference<Instant> now) {
        return new Clock() {
            @Override
            public Instant instant() {
                return now.get();
            }

            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }
        };
    }

    private static final AtomicReference<Instant> NOW =
            new AtomicReference<>(Instant.parse("2026-09-12T00:00:00Z"));

    private void advanceMinutes(int minutes) {
        NOW.set(NOW.get().plusSeconds(minutes * 60L));
    }

    @Test
    void spikeFiresAfterSufficientBaseline() {
        NOW.set(Instant.parse("2026-09-12T00:00:00Z"));
        SpendRateRing ring = new SpendRateRing(40, mutableClock(NOW));
        CostSpikeDetector detector = new CostSpikeDetector(ring, 30, 10,
                3.0, 10_000L, Duration.ofMinutes(5), mutableClock(NOW));
        List<CostSpikeDetector.SpikeEvent> events = new ArrayList<>();
        detector.onSpike(events::add);

        // 12 个基线分钟桶，每桶 1000 microUsd（低于地板——只做基线）
        for (int i = 0; i < 12; i++) {
            advanceMinutes(1);
            detector.record(1_000);
        }
        assertThat(events).isEmpty(); // 地板下不触发

        // 突刺桶：500_000 ≫ 基线 1000±0
        advanceMinutes(1);
        detector.record(500_000);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).currentBucketMicroUsd()).isEqualTo(500_000);
        assertThat(events.get(0).zScore()).isGreaterThan(3.0);
        assertThat(detector.spikeCount()).isEqualTo(1);
    }

    @Test
    void insufficientBaselineNeverFires() {
        NOW.set(Instant.parse("2026-09-12T00:00:00Z"));
        SpendRateRing ring = new SpendRateRing(40, mutableClock(NOW));
        CostSpikeDetector detector = new CostSpikeDetector(ring, 30, 10,
                3.0, 10_000L, Duration.ofMinutes(5), mutableClock(NOW));
        List<CostSpikeDetector.SpikeEvent> events = new ArrayList<>();
        detector.onSpike(events::add);
        // 仅 3 个基线桶就突刺
        for (int i = 0; i < 3; i++) {
            advanceMinutes(1);
            detector.record(1_000);
        }
        advanceMinutes(1);
        detector.record(500_000);
        assertThat(events).isEmpty();
    }

    @Test
    void cooldownSuppressesRepeatSpikes() {
        NOW.set(Instant.parse("2026-09-12T00:00:00Z"));
        SpendRateRing ring = new SpendRateRing(40, mutableClock(NOW));
        CostSpikeDetector detector = new CostSpikeDetector(ring, 30, 10,
                3.0, 10_000L, Duration.ofMinutes(5), mutableClock(NOW));
        List<CostSpikeDetector.SpikeEvent> events = new ArrayList<>();
        detector.onSpike(events::add);
        for (int i = 0; i < 12; i++) {
            advanceMinutes(1);
            detector.record(1_000);
        }
        advanceMinutes(1);
        detector.record(500_000);
        // 冷却窗内（5min）第二突刺
        advanceMinutes(2);
        detector.record(800_000);
        assertThat(events).hasSize(1); // 防抖
        // 冷却窗过后再触发
        advanceMinutes(6);
        detector.record(900_000);
        assertThat(events).hasSize(2);
    }

    @Test
    void ymlAssemblyOnlyWhenEnabled() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues("buzhou.budget.spike.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouCostSpikeDetector");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouCostSpikeDetector");
                });
    }
}
