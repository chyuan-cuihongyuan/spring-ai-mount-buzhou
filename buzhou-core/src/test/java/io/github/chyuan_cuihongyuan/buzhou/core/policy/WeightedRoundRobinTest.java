package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5026 / T6154：平滑 WRR 合同——经典序列逐位、权重比
 * 精确、单节点退化、畸形 fail-fast、确定性。
 */
class WeightedRoundRobinTest {

    @Test
    void classicFiveOneOneShouldProduceSmoothSequence() {
        // LinkedHashMap 固定注册序——并列 tie-break 确定性（b 先于 c）
        Map<String, Integer> weights = new java.util.LinkedHashMap<>();
        weights.put("a", 5);
        weights.put("b", 1);
        weights.put("c", 1);
        WeightedRoundRobin wrr = new WeightedRoundRobin(weights);
        List<String> expected = List.of("a", "a", "b", "a", "c", "a", "a");   // nginx 经典例
        for (String expectedNode : expected) {
            assertThat(wrr.next()).isEqualTo(expectedNode);
        }
    }

    @Test
    void frequenciesShouldMatchWeightRatio() {
        WeightedRoundRobin wrr = new WeightedRoundRobin(Map.of("a", 5, "b", 1, "c", 1));
        int rounds = 35;   // 5 组
        int a = 0;
        int b = 0;
        int c = 0;
        for (int i = 0; i < rounds; i++) {
            switch (wrr.next()) {
                case "a" -> a++;
                case "b" -> b++;
                case "c" -> c++;
                default -> throw new IllegalStateException("未知节点");
            }
        }
        assertThat(a).isEqualTo(25);   // 5/7 × 35
        assertThat(b).isEqualTo(5);
        assertThat(c).isEqualTo(5);
    }

    @Test
    void singleNodeShouldAlwaysWin() {
        WeightedRoundRobin wrr = new WeightedRoundRobin(Map.of("solo", 3));
        for (int i = 0; i < 6; i++) {
            assertThat(wrr.next()).isEqualTo("solo");
        }
    }

    @Test
    void sameConfigShouldReplaySameSequence() {
        Map<String, Integer> weights = Map.of("a", 2, "b", 1);
        WeightedRoundRobin first = new WeightedRoundRobin(weights);
        WeightedRoundRobin second = new WeightedRoundRobin(new HashMap<>(weights));
        for (int i = 0; i < 9; i++) {
            assertThat(first.next()).isEqualTo(second.next());
        }
    }

    @Test
    void invalidWeightsShouldFailFast() {
        assertThatThrownBy(() -> new WeightedRoundRobin(Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WeightedRoundRobin(Map.of("a", 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
