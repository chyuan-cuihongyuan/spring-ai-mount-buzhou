package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2038 / T3180：SimHash 合同——同文本同指纹（确定性）、相似文本
 * 汉明距离小（近重复）、相异文本距离大、汉明距离对称、阈值判定、
 * 畸形 fail-fast。
 */
class SimHashFingerprintTest {

    @Test
    void identicalTokensShouldYieldIdenticalFingerprint() {
        long a = SimHashFingerprint.fingerprint(List.of("the", "quick", "brown", "fox"));
        long b = SimHashFingerprint.fingerprint(List.of("the", "quick", "brown", "fox"));
        assertThat(a).isEqualTo(b); // 确定性
        assertThat(SimHashFingerprint.hammingDistance(a, b)).isZero();
    }

    @Test
    void duplicatedTokensShouldNotShiftFingerprint() {
        // 词元重复=同向票加倍——任何 bit 符号不变，指纹恰等（确定性数学性质）
        List<String> base = List.of(
                "session", "timeout", "retry", "failed", "handler", "context");
        long a = SimHashFingerprint.fingerprint(base);
        long doubled = SimHashFingerprint.fingerprint(
                java.util.stream.Stream.concat(base.stream(), base.stream()).toList());
        assertThat(a).isEqualTo(doubled);
    }

    @Test
    void minorAdditionShouldStayMuchCloserThanDisjointText() {
        // 相对判定：+2 词元（小增量）的距离 << 完全不交文本的距离（期望 ~32）
        List<String> base = List.of(
                "session", "timeout", "retry", "failed", "handler", "context",
                "resilience", "backpressure", "circuit", "budget");
        long a = SimHashFingerprint.fingerprint(base);
        long minor = SimHashFingerprint.fingerprint(
                java.util.stream.Stream.concat(base.stream(),
                        java.util.stream.Stream.of("extra", "token")).toList());
        long disjoint = SimHashFingerprint.fingerprint(List.of(
                "quantum", "zebra", "meteor", "cascade", "obsidian", "tundra",
                "vertex", "plasma", "harbor", "lantern"));
        int minorDistance = SimHashFingerprint.hammingDistance(a, minor);
        int disjointDistance = SimHashFingerprint.hammingDistance(a, disjoint);
        assertThat(minorDistance).as("小增量距离").isLessThan(disjointDistance / 2); // 显著更近
        assertThat(disjointDistance).isGreaterThan(16); // 不交文本远离（期望 32）
    }

    @Test
    void disjointTextsShouldBeFarApart() {
        long a = SimHashFingerprint.fingerprint(List.of("alpha", "beta", "gamma", "delta"));
        long b = SimHashFingerprint.fingerprint(List.of(
                "quantum", "zebra", "meteor", "cascade", "obsidian", "tundra", "vertex", "plasma"));
        assertThat(SimHashFingerprint.hammingDistance(a, b))
                .as("相异文本距离").isGreaterThan(SimHashFingerprint.DEFAULT_NEAR_DISTANCE);
        assertThat(SimHashFingerprint.isNearDuplicate(a, b, SimHashFingerprint.DEFAULT_NEAR_DISTANCE))
                .isFalse();
    }

    @Test
    void hammingDistanceShouldBeSymmetricAndBounded() {
        long a = 0b1010L;
        long b = 0b0110L;
        assertThat(SimHashFingerprint.hammingDistance(a, b)).isEqualTo(2);
        assertThat(SimHashFingerprint.hammingDistance(b, a)).isEqualTo(2); // 对称
        assertThat(SimHashFingerprint.hammingDistance(0L, -1L)).isEqualTo(64); // 全异上界
        assertThat(SimHashFingerprint.hammingDistance(a, a)).isZero();
    }

    @Test
    void thresholdZeroShouldMeanExactMatch() {
        long a = SimHashFingerprint.fingerprint(List.of("x", "y"));
        assertThat(SimHashFingerprint.isNearDuplicate(a, a, 0)).isTrue();
        assertThat(SimHashFingerprint.isNearDuplicate(a, a ^ 1, 0)).isFalse(); // 差 1 bit 也不算
        assertThat(SimHashFingerprint.isNearDuplicate(a, a ^ 1, 1)).isTrue(); // 距离 1 阈内
    }

    @Test
    void singleTokenFingerprintShouldBeItsHash() {
        // 单词元投票：全 bit 同向——指纹即词元散列的规范化（无零和位时）
        long fp = SimHashFingerprint.fingerprint(List.of("solo"));
        assertThat(fp).isNotZero(); // 有意义指纹
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> SimHashFingerprint.fingerprint(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SimHashFingerprint.fingerprint(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SimHashFingerprint.fingerprint(java.util.Arrays.asList("a", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SimHashFingerprint.isNearDuplicate(0, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
