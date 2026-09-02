package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 321 / impl-344：错误预算回归——burn 计算/breach 判定/min-samples 门/
 * 窗旋转过期/overflow 折叠/构造校验（MutableClock 先例 131）。
 */
class ErrorBudgetTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-02T00:00:00Z");

        void advanceMillis(long ms) {
            now = now.plusMillis(ms);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /** 快测配置：SLO 99% / burn 阈 2 / 4 桶 × 200ms 窗 / min-samples 2。 */
    private static ErrorBudget.Config fast() {
        return new ErrorBudget.Config(99, 2.0, 4, Duration.ofMillis(200), 2);
    }

    @Test
    void burnComputationAndBreachDetection() {
        ErrorBudget budget = new ErrorBudget(fast(), new MutableClock());
        budget.record("t", false); // 1 败
        for (int i = 0; i < 19; i++) {
            budget.record("t", true); // 19 成 → 错误率 5%
        }
        assertThat(budget.errorRate("t")).isCloseTo(0.05, within(1e-9));
        assertThat(budget.burnRate("t"))
                .as("0.05/(1-0.99) = 5×——五倍速烧预算").isCloseTo(5.0, within(1e-9));
        assertThat(budget.breaching("t")).isTrue();
        assertThat(budget.anyBreaching()).isTrue();
        assertThat(budget.topBreaching(3)).hasSize(1);
        assertThat(budget.topBreaching(3).get(0).getKey()).isEqualTo("t");
        assertThat(budget.topBreaching(3).get(0).getValue())
                .isCloseTo(5.0, within(1e-9));
    }

    @Test
    void healthyBudgetStaysUp() {
        ErrorBudget budget = new ErrorBudget(fast(), new MutableClock());
        for (int i = 0; i < 20; i++) {
            budget.record("t", true);
        }
        assertThat(budget.breaching("t")).isFalse();
        assertThat(budget.anyBreaching()).isFalse();
        assertThat(budget.hasSamples()).isTrue();
    }

    @Test
    void minSamplesGateBlocksOneOffNoise() {
        ErrorBudget budget = new ErrorBudget(
                new ErrorBudget.Config(99, 2.0, 4, Duration.ofMillis(200), 20),
                new MutableClock());
        for (int i = 0; i < 10; i++) {
            budget.record("t", false); // 100% 错误率但样本 10 < 20
        }
        assertThat(budget.samples("t")).isEqualTo(10);
        assertThat(budget.breaching("t"))
                .as("「一败 100%」噪声不触发——样本须 ≥ min-samples").isFalse();
    }

    @Test
    void windowRotationExpiresOldSamples() {
        MutableClock clock = new MutableClock();
        ErrorBudget budget = new ErrorBudget(fast(), clock);
        budget.record("t", false);
        budget.record("t", false);
        assertThat(budget.breaching("t")).isTrue();
        clock.advanceMillis(200); // 窗整体滑出
        assertThat(budget.samples("t")).isZero();
        assertThat(budget.errorRate("t")).isZero();
        assertThat(budget.breaching("t")).isFalse();
    }

    @Test
    void overflowFoldsBeyond256Scopes() {
        ErrorBudget budget = new ErrorBudget(
                new ErrorBudget.Config(99, 2.0, 4, Duration.ofMillis(200), 1),
                new MutableClock());
        for (int i = 0; i < 300; i++) {
            budget.record("tool-" + i, true);
        }
        assertThat(budget.trackedScopes())
                .as("256 封顶 + __overflow__ 折叠行").isEqualTo(257);
    }

    @Test
    void constructorValidates() {
        MutableClock clock = new MutableClock();
        assertThatThrownBy(() -> new ErrorBudget(
                new ErrorBudget.Config(0, 2, 4, Duration.ofMillis(200), 2), clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ErrorBudget(
                new ErrorBudget.Config(100, 2, 4, Duration.ofMillis(200), 2), clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ErrorBudget(
                new ErrorBudget.Config(99, 0, 4, Duration.ofMillis(200), 2), clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ErrorBudget(
                new ErrorBudget.Config(99, 2, 1, Duration.ofMillis(200), 2), clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ErrorBudget(
                new ErrorBudget.Config(99, 2, 4, Duration.ZERO, 2), clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ErrorBudget(
                new ErrorBudget.Config(99, 2, 4, Duration.ofMillis(200), 0), clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ErrorBudget(fast(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
