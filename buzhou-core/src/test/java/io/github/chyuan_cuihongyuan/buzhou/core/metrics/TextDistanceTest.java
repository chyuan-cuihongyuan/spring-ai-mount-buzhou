package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 2052 / T3206：编辑距离合同——经典用例、空串、对称、相似比、
 * 近匹配阈值、畸形 fail-fast。
 */
class TextDistanceTest {

    @Test
    void classicDistanceShouldMatchTextbook() {
        assertThat(TextDistance.levenshtein("kitten", "sitting")).isEqualTo(3);
        assertThat(TextDistance.levenshtein("flaw", "lawn")).isEqualTo(2);
        assertThat(TextDistance.levenshtein("intention", "execution")).isEqualTo(5);
    }

    @Test
    void identicalAndEmptyShouldBeTrivial() {
        assertThat(TextDistance.levenshtein("same", "same")).isZero();
        assertThat(TextDistance.levenshtein("", "")).isZero();
        assertThat(TextDistance.levenshtein("", "abc")).isEqualTo(3); // 全插
        assertThat(TextDistance.levenshtein("abcd", "")).isEqualTo(4); // 全删
    }

    @Test
    void distanceShouldBeSymmetric() {
        assertThat(TextDistance.levenshtein("abcde", "azced"))
                .isEqualTo(TextDistance.levenshtein("azced", "abcde"));
    }

    @Test
    void completelyDifferentShouldHitMaxLen() {
        assertThat(TextDistance.levenshtein("aaa", "bbb")).isEqualTo(3); // 全改
        assertThat(TextDistance.similarity("aaa", "bbb")).isZero();
    }

    @Test
    void similarityShouldNormalizeByLength() {
        assertThat(TextDistance.similarity("", "")).isEqualTo(1.0);
        assertThat(TextDistance.similarity("same", "same")).isEqualTo(1.0);
        // dist 1 / len 5 → 0.8
        assertThat(TextDistance.similarity("abcde", "abcdX")).isCloseTo(0.8d, within(1e-12));
    }

    @Test
    void nearMatchThresholdShouldDecide() {
        assertThat(TextDistance.isNearMatch("buzhou.core.enabled", "buzhou.core.enable", 0.8)).isTrue();
        assertThat(TextDistance.isNearMatch("buzhou.core.enabled", "totally.different.key", 0.8)).isFalse();
        assertThat(TextDistance.isNearMatch("a", "b", 0.0)).isTrue();  // 零阈全过
        assertThat(TextDistance.isNearMatch("a", "a", 1.0)).isTrue();  // 满阈须全同
        assertThat(TextDistance.isNearMatch("a", "b", 1.0)).isFalse();
    }

    @Test
    void longStringsShouldComputeCorrectly() {
        String a = "a".repeat(500);
        String b = "a".repeat(499) + "b";
        assertThat(TextDistance.levenshtein(a, b)).isEqualTo(1);
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> TextDistance.levenshtein(null, "a"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TextDistance.levenshtein("a", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TextDistance.similarity(null, "a"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TextDistance.isNearMatch("a", "b", 1.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TextDistance.isNearMatch("a", "b", -0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
