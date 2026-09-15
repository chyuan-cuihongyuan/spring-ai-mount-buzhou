package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1831 / T2864：对冲延迟——最近秩分位、样本不足退守、边界裁决。 */
class HedgeDelayPolicyTest {

    /** 最近秩 P95：40 样本 rank=ceil(0.95×40)=38 → 第 38 小。 */
    @Test
    void nearestRankPercentileFromSufficientSamples() {
        List<Long> samples = IntStream.rangeClosed(1, 40)
                .mapToLong(i -> i * 10L).boxed().toList();
        long threshold = HedgeDelayPolicy.hedgeThresholdMillis(samples,
                HedgeDelayPolicy.DEFAULT_PERCENTILE, HedgeDelayPolicy.DEFAULT_FLOOR_MILLIS,
                HedgeDelayPolicy.DEFAULT_MIN_SAMPLES);
        assertThat(threshold).isEqualTo(380L);
    }

    /** 样本不足退守地板；乱序输入同结果（内部排序）。 */
    @Test
    void insufficientSamplesFallBackToFloorsAndOrderTolerant() {
        List<Long> few = List.of(5L, 999L);
        assertThat(HedgeDelayPolicy.hedgeThresholdMillis(few, 0.95d, 25L,
                HedgeDelayPolicy.DEFAULT_MIN_SAMPLES)).isEqualTo(25L);

        List<Long> shuffled = IntStream.rangeClosed(1, 25)
                .mapToLong(i -> 26L - i).boxed().toList();
        assertThat(HedgeDelayPolicy.hedgeThresholdMillis(shuffled, 0.5d, 0L,
                HedgeDelayPolicy.DEFAULT_MIN_SAMPLES)).isEqualTo(13L);
    }

    /** 裁决边界含上：elapsed == threshold 即发对冲。 */
    @Test
    void decideBoundaryIsInclusive() {
        assertThat(HedgeDelayPolicy.decide(99, 100)).isEqualTo(HedgeDelayPolicy.HedgeDecision.WAIT);
        assertThat(HedgeDelayPolicy.decide(100, 100))
                .isEqualTo(HedgeDelayPolicy.HedgeDecision.SEND_HEDGE);
        assertThat(HedgeDelayPolicy.decide(0, 0))
                .isEqualTo(HedgeDelayPolicy.HedgeDecision.SEND_HEDGE);
    }

    /** 畸形入参 fail-fast：空样本、分位越界、负地板、负样本、负裁决入参。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> HedgeDelayPolicy.hedgeThresholdMillis(List.of(), 0.95d, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("samples 不能为空");
        assertThatThrownBy(() -> HedgeDelayPolicy.hedgeThresholdMillis(List.of(1L), 0d, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("percentile 须在 (0,1]");
        assertThatThrownBy(() -> HedgeDelayPolicy.hedgeThresholdMillis(List.of(1L), 0.95d, -1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HedgeDelayPolicy.decide(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HedgeDelayPolicy.hedgeThresholdMillis(
                java.util.Arrays.asList(1L, null), 0.95d, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
