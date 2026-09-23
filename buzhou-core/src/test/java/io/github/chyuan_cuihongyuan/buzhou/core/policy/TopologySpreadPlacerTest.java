package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4024 / T6050：拓扑散布合同——均衡全可、偏斜截止、
 * 严档、斜度读数、畸形 fail-fast。
 */
class TopologySpreadPlacerTest {

    @Test
    void balancedDomainsShouldAllBeEligible() {
        TopologySpreadPlacer placer = new TopologySpreadPlacer(2);
        Map<String, Integer> counts = Map.of("a", 2, "b", 2, "c", 2);
        assertThat(placer.eligibleDomains(counts)).containsExactly("a", "b", "c");
        assertThat(placer.canPlace(counts, "b")).isTrue();
        assertThat(placer.skewOf(counts)).isZero();
    }

    @Test
    void crowdedDomainShouldBeBlockedAtSkewBoundary() {
        TopologySpreadPlacer placer = new TopologySpreadPlacer(2);
        Map<String, Integer> counts = Map.of("a", 5, "b", 3);
        assertThat(placer.canPlace(counts, "b")).isTrue();   // 放后 5-3=2 恰界
        assertThat(placer.canPlace(counts, "a")).isFalse();   // 放后 6-3=3 越界
        assertThat(placer.eligibleDomains(counts)).containsExactly("b");
    }

    @Test
    void strictSkewShouldForceAlternatingPlacement() {
        TopologySpreadPlacer placer = new TopologySpreadPlacer(1);
        Map<String, Integer> counts = new java.util.HashMap<>(Map.of("a", 3, "b", 2));
        assertThat(placer.eligibleDomains(counts)).containsExactly("b");   // 只能填最空
        counts.put("b", 3);
        assertThat(placer.eligibleDomains(counts)).containsExactly("a", "b");   // 追平后双开
        assertThat(placer.skewOf(counts)).isZero();
    }

    @Test
    void leastLoadedFirstOrderShouldBeDeterministic() {
        TopologySpreadPlacer placer = new TopologySpreadPlacer(3);
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        counts.put("z", 4);
        counts.put("m", 1);
        counts.put("a", 2);
        assertThat(placer.eligibleDomains(counts)).containsExactly("m", "a");   // 数升序 + z 域放置后斜度 4>3 被截止
        assertThat(placer.maxSkew()).isEqualTo(3);
        assertThat(placer.skewOf(counts)).isEqualTo(3);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new TopologySpreadPlacer(0))
                .isInstanceOf(IllegalArgumentException.class);
        TopologySpreadPlacer placer = new TopologySpreadPlacer(1);
        assertThatThrownBy(() -> placer.eligibleDomains(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> placer.eligibleDomains(Map.of("a", -1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> placer.canPlace(Map.of("a", 1), "missing"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> placer.canPlace(Map.of("a", 1), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(placer.skewOf(Map.of())).isZero();   // 空图诚实 0
    }
}
