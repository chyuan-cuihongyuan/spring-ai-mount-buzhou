package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** spec 1839 / T2880：反熵分歧——三型分开数、修复工作量、一致率。 */
class AntiEntropyDivergenceTest {

    /** 三型分歧+一致四桶分开数，工作量=三型合计。 */
    @Test
    void shouldBucketThreeDivergenceTypes() {
        AntiEntropyDivergence.Divergence d = AntiEntropyDivergence.compare(
                Map.of("k1", 1L, "k2", 2L, "k3", 3L, "k4", 4L),
                Map.of("k1", 1L, "k2", 9L, "k5", 5L));
        assertThat(d.onlyInPrimary()).isEqualTo(2L); // k3 k4
        assertThat(d.onlyInReplica()).isEqualTo(1L); // k5
        assertThat(d.versionMismatch()).isEqualTo(1L); // k2
        assertThat(d.matched()).isEqualTo(1L); // k1
        assertThat(d.repairWorkload()).isEqualTo(4L);
        assertThat(d.matchedRatio()).isEqualTo(0.2d);
    }

    /** 全一致：工作量 0、一致率 1。 */
    @Test
    void fullyMatchedReadsHealthy() {
        AntiEntropyDivergence.Divergence d = AntiEntropyDivergence.compare(
                Map.of("a", 1L, "b", 2L), Map.of("a", 1L, "b", 2L));
        assertThat(d.repairWorkload()).isZero();
        assertThat(d.matchedRatio()).isEqualTo(1.0d);
    }

    /** 空比对与单侧空：哨兵与全量分歧。 */
    @Test
    void emptyComparisonsBehave() {
        assertThat(AntiEntropyDivergence.compare(Map.of(), Map.of()).matchedRatio())
                .isEqualTo(-1d);
        assertThat(AntiEntropyDivergence.compare(null, null).matchedRatio())
                .isEqualTo(-1d);
        AntiEntropyDivergence.Divergence oneSided = AntiEntropyDivergence.compare(
                Map.of("a", 1L), Map.of());
        assertThat(oneSided.onlyInPrimary()).isEqualTo(1L);
        assertThat(oneSided.repairWorkload()).isEqualTo(1L);
    }
}
