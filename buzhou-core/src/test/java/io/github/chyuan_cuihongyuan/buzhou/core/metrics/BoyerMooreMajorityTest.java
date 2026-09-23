package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4003 / T6008：多数表决合同——真多数幸存、恰半僵局 null、
 * 抵消重立、空表/单元素、畸形 fail-fast。
 */
class BoyerMooreMajorityTest {

    @Test
    void trueMajorityShouldSurviveCancellation() {
        assertThat(BoyerMooreMajority.majorityOf(
                List.of("a", "a", "b", "a", "c", "a"))).isEqualTo("a");   // 4/6 > 1/2
    }

    @Test
    void exactHalfShouldBeStalemateNotMajority() {
        assertThat(BoyerMooreMajority.majorityOf(List.of("a", "a", "b", "b"))).isNull();
        assertThat(BoyerMooreMajority.majorityOf(List.of("a", "b", "c"))).isNull();
    }

    @Test
    void candidateShouldReEstablishAfterFullCancellation() {
        // a+1 → b 抵消 → b 立 → a 抵消 → a 立；核验 a=3 > 5/2
        assertThat(BoyerMooreMajority.majorityOf(List.of("a", "b", "b", "a", "a")))
                .isEqualTo("a");
        // 首元素全程被围剿：x=1 其余均异——核验否决
        assertThat(BoyerMooreMajority.majorityOf(List.of("x", "a", "b", "c", "d"))).isNull();
    }

    @Test
    void emptyAndSingletonShouldBehave() {
        assertThat(BoyerMooreMajority.majorityOf(List.of())).isNull();
        assertThat(BoyerMooreMajority.majorityOf(List.of("only"))).isEqualTo("only");
        assertThat(BoyerMooreMajority.majorityOf(List.of(7, 7, 9))).isEqualTo(7);   // 泛型直证
    }

    @Test
    void nullListShouldFailFast() {
        assertThatThrownBy(() -> BoyerMooreMajority.majorityOf(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
