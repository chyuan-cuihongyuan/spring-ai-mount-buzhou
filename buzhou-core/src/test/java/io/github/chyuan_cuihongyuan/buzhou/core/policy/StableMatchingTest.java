package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4027 / T6056：稳定匹配合同——冲突换优、求婚方最优、
 * 不齐边诚实、稳定性对账、畸形 fail-fast。
 */
class StableMatchingTest {

    @Test
    void conflictShouldResolveByAcceptorPreference() {
        Map<String, String> matched = StableMatching.match(
                Map.of("A", List.of("x", "y"), "B", List.of("x", "y")),
                Map.of("x", List.of("B", "A"), "y", List.of("B", "A")));
        // A 先占 x；B 求x——x 偏好 B 换优踢 A；A 转 y
        assertThat(matched).containsEntry("A", "y").containsEntry("B", "x");
        assertThat(matched).hasSize(2);
    }

    @Test
    void proposersShouldGetOptimalStableOutcome() {
        Map<String, String> matched = StableMatching.match(
                Map.of("A", List.of("y", "x"), "B", List.of("x", "y")),
                Map.of("x", List.of("A", "B"), "y", List.of("B", "A")));
        assertThat(matched).containsEntry("A", "y").containsEntry("B", "x");   // 双方首选
    }

    @Test
    void unevenSidesShouldLeaveExtrasUnmatched() {
        Map<String, String> matched = StableMatching.match(
                Map.of("A", List.of("x", "y", "z"), "B", List.of("x", "y", "z")),
                Map.of("x", List.of("A", "B"), "y", List.of("B", "A"),
                        "z", List.of("A", "B")));
        assertThat(matched).hasSize(2);   // 求婚方全配，z 闲置（不在结果值中）
        assertThat(matched.containsValue("z")).isFalse();
    }

    @Test
    void exhaustedWishlistShouldStayUnmatched() {
        Map<String, String> matched = StableMatching.match(
                Map.of("A", List.of("x"), "B", List.of("x")),
                Map.of("x", List.of("B", "A")));
        assertThat(matched).containsExactly(Map.entry("B", "x"));   // A 链尽不配
    }

    @Test
    void resultShouldBeStableNoBlockingPair() {
        Map<String, List<String>> proposers = Map.of(
                "A", List.of("x", "y", "z"), "B", List.of("x", "y", "z"),
                "C", List.of("y", "x", "z"));
        Map<String, List<String>> acceptors = Map.of(
                "x", List.of("C", "A", "B"), "y", List.of("B", "C", "A"),
                "z", List.of("A", "B", "C"));
        Map<String, String> matched = StableMatching.match(proposers, acceptors);
        assertThat(matched).hasSize(3);
        for (String p : proposers.keySet()) {
            for (String a : acceptors.keySet()) {
                if (matched.get(p).equals(a)) {
                    continue;
                }
                String pPartner = matched.get(p);
                String aPartner = keyOf(matched, a);
                boolean pPrefers = proposers.get(p).indexOf(a) < proposers.get(p).indexOf(pPartner);
                boolean aPrefers = acceptors.get(a).indexOf(p) < acceptors.get(a).indexOf(aPartner);
                assertThat(pPrefers && aPrefers)
                        .as("阻塞对 (%s,%s) 不应存在", p, a).isFalse();
            }
        }
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> StableMatching.match(null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StableMatching.match(Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
        Map<String, List<String>> nullPrefProposer = new java.util.HashMap<>();
        nullPrefProposer.put("A", null);
        assertThatThrownBy(() -> StableMatching.match(nullPrefProposer, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        Map<String, List<String>> nullPrefAcceptor = new java.util.HashMap<>();
        nullPrefAcceptor.put("x", null);
        assertThatThrownBy(() -> StableMatching.match(Map.of(), nullPrefAcceptor))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(StableMatching.match(Map.of(), Map.of())).isEmpty();
    }

    private static String keyOf(Map<String, String> matched, String value) {
        return matched.entrySet().stream()
                .filter(e -> e.getValue().equals(value))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }
}
