package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 818 / T1138：自适应收紧回归——收紧/下限/保持窗/步进恢复/封顶 1.0/
 * 确定性/模型隔离/封顶 32/fail-fast。
 */
class AdaptiveRateTightenerTest {

    /** 0.5 收缩、保持 1000ms、每 500ms 恢复 ×2。 */
    private AdaptiveRateTightener tightener() {
        return new AdaptiveRateTightener(0.05, 0.5, 1_000, 500, 2.0);
    }

    @Test
    void tightenHoldAndGradualRecovery() {
        AdaptiveRateTightener t = tightener();
        assertThat(t.effectiveMultiplier("m", 0)).isEqualTo(1.0); // 未收紧

        t.onThrottled("m", 1_000);
        assertThat(t.effectiveMultiplier("m", 1_500)).isEqualTo(0.5); // 保持期内
        assertThat(t.isTightened("m", 1_500)).isTrue();

        // 保持期后步进恢复：步从保持窗结束起算——over=now-1000-1000
        assertThat(t.effectiveMultiplier("m", 2_001)).isEqualTo(0.5);   // over=1 → 0 步
        assertThat(t.effectiveMultiplier("m", 2_500)).isEqualTo(1.0);   // over=500 → 1 步 ×2 封顶
        assertThat(t.isTightened("m", 2_500)).isFalse();
    }

    @Test
    void repeatedThrottlesShrinkToFloor() {
        AdaptiveRateTightener t = new AdaptiveRateTightener(0.05, 0.5, 0, 500, 2.0);
        long now = 0;
        for (int i = 0; i < 10; i++) {
            now += 10_000; // 每次都过了恢复期（但收紧发生在每次当下）
            t.onThrottled("m", now);
        }
        // 0.5^10 = 0.000976 < 下限 0.05
        assertThat(t.effectiveMultiplier("m", now)).isEqualTo(0.05);
        assertThat(t.effectiveMultiplier("m", now)).isEqualTo(0.05); // 同参同值——确定性
    }

    @Test
    void gradualMultiStepRecovery() {
        AdaptiveRateTightener t = new AdaptiveRateTightener(0.05, 0.25, 1_000, 1_000, 2.0);
        t.onThrottled("m", 0); // 0.25
        assertThat(t.effectiveMultiplier("m", 1_000)).isEqualTo(0.25); // 保持恰满
        assertThat(t.effectiveMultiplier("m", 1_999)).isEqualTo(0.25); // 未满一步
        assertThat(t.effectiveMultiplier("m", 2_000)).isEqualTo(0.5);  // 1 步
        assertThat(t.effectiveMultiplier("m", 3_000)).isEqualTo(1.0);  // 2 步封顶
    }

    @Test
    void modelsAreIndependent() {
        AdaptiveRateTightener t = tightener();
        t.onThrottled("m1", 0);
        assertThat(t.isTightened("m1", 100)).isTrue();
        assertThat(t.isTightened("m2", 100)).isFalse();
        assertThat(t.effectiveMultiplier(null, 100)).isEqualTo(1.0);
        t.onThrottled(null, 0); // 忽略不抛
        t.onThrottled("  ", 0);
    }

    @Test
    void modelCapTruncates() {
        AdaptiveRateTightener t = tightener();
        for (int i = 0; i < AdaptiveRateTightener.MAX_MODELS + 3; i++) {
            t.onThrottled("x" + i, 0);
        }
        assertThat(t.truncated()).isTrue();
        assertThat(t.isTightened("x0", 1)).isTrue();
        assertThat(t.isTightened("x" + (AdaptiveRateTightener.MAX_MODELS + 2), 1)).isFalse(); // 超封顶未建态
    }

    @Test
    void failFastOnBadParams() {
        assertThatThrownBy(() -> new AdaptiveRateTightener(0, 0.5, 100, 500, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AdaptiveRateTightener(0.5, 1.0, 100, 500, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AdaptiveRateTightener(0.5, 0.5, -1, 500, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AdaptiveRateTightener(0.5, 0.5, 100, 0, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AdaptiveRateTightener(0.5, 0.5, 100, 500, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
