package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import java.util.Random;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.TailSamplingPolicy.Verdict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4022 / T6046：尾采样合同——错误必采、慢必采含等、
 * 概率分支、预算门、畸形 fail-fast。
 */
class TailSamplingPolicyTest {

    @Test
    void errorTraceShouldAlwaysBeSampledRegardlessOfBudget() {
        TailSamplingPolicy policy = new TailSamplingPolicy(1000, 0.0, 0, new Random(1));
        Verdict verdict = policy.evaluate(5, true);   // 概率 0 + 预算 0 也不拦错误
        assertThat(verdict.sampled()).isTrue();
        assertThat(verdict.reason()).isEqualTo("error");
        assertThat(policy.errorSampled()).isEqualTo(1);
        assertThat(policy.probabilisticSampled()).isZero();
    }

    @Test
    void slowTraceShouldBeSampledAtThresholdInclusive() {
        TailSamplingPolicy policy = new TailSamplingPolicy(1000, 0.0, 0, new Random(1));
        assertThat(policy.evaluate(999, false).sampled()).isFalse();   // 阈下
        Verdict at = policy.evaluate(1000, false);   // 恰阈值——含等
        assertThat(at.sampled()).isTrue();
        assertThat(at.reason()).isEqualTo("slow");
        assertThat(policy.evaluate(5000, false).reason()).isEqualTo("slow");
        assertThat(policy.slowSampled()).isEqualTo(2);
    }

    @Test
    void probabilisticBranchShouldFollowSeededRandom() {
        TailSamplingPolicy all = new TailSamplingPolicy(1000, 1.0, 10, new Random(1));
        assertThat(all.evaluate(100, false)).isEqualTo(new Verdict(true, "probabilistic"));
        TailSamplingPolicy none = new TailSamplingPolicy(1000, 0.0, 10, new Random(1));
        assertThat(none.evaluate(100, false)).isEqualTo(new Verdict(false, "below-threshold"));
        TailSamplingPolicy half = new TailSamplingPolicy(1000, 0.5, 1000, new Random(42));
        long sampled = 0;
        for (int i = 0; i < 1000; i++) {
            if (half.evaluate(100, false).sampled()) {
                sampled++;
            }
        }
        assertThat(sampled).isBetween(400L, 600L);   // 种子确定——近似半数
        assertThat(half.probabilisticSampled()).isEqualTo(sampled);
        assertThat(half.dropped()).isEqualTo(1000 - sampled);
    }

    @Test
    void budgetShouldCapProbabilisticButNotGoldenChannels() {
        TailSamplingPolicy policy = new TailSamplingPolicy(1000, 1.0, 2, new Random(1));
        assertThat(policy.evaluate(100, false).reason()).isEqualTo("probabilistic");
        assertThat(policy.evaluate(100, false).reason()).isEqualTo("probabilistic");
        assertThat(policy.evaluate(100, false).reason()).isEqualTo("budget");   // 预算尽
        assertThat(policy.evaluate(100, false).sampled()).isFalse();
        assertThat(policy.evaluate(5, true).sampled()).isTrue();   // 错误通道不受预算影响
        assertThat(policy.evaluate(2000, false).sampled()).isTrue();   // 慢通道不受预算影响
        assertThat(policy.probabilisticSampled()).isEqualTo(2);
        assertThat(policy.dropped()).isEqualTo(2);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new TailSamplingPolicy(-1, 0.5, 10, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TailSamplingPolicy(1000, -0.1, 10, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TailSamplingPolicy(1000, 1.1, 10, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TailSamplingPolicy(1000, 0.5, -1, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TailSamplingPolicy(1000, 0.5, 10, null))
                .isInstanceOf(IllegalArgumentException.class);
        TailSamplingPolicy policy = new TailSamplingPolicy(1000, 0.5, 10, new Random());
        assertThatThrownBy(() -> policy.evaluate(-1, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
