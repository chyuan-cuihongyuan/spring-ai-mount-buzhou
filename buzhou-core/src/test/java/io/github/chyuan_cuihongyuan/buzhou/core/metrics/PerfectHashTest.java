package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerfectHashTest {

    @Test
    void shouldGiveBijectionForSmallKeySet() {
        PerfectHash hash = PerfectHash.build(List.of("a", "b", "c"), 42);
        Set<Integer> slots = new HashSet<>();
        for (String key : List.of("a", "b", "c")) {
            int slot = hash.lookup(key);
            assertThat(slot).isBetween(0, 2);
            slots.add(slot);
        }
        assertThat(slots).hasSize(3);
        assertThat(hash.lookup("zz")).isEqualTo(-1);
        assertThat(hash.size()).isEqualTo(3);
    }

    @Test
    void shouldBeBijectiveOnRandomKeySets() {
        Random random = new Random(8013);
        for (int round = 0; round < 300; round++) {
            int n = 1 + random.nextInt(60);
            Set<String> distinct = new HashSet<>();
            while (distinct.size() < n) {
                distinct.add("k" + random.nextInt(200));
            }
            List<String> keys = new ArrayList<>(distinct);
            PerfectHash hash = PerfectHash.build(keys, 7);
            Set<Integer> slots = new HashSet<>();
            for (String key : keys) {
                int slot = hash.lookup(key);
                assertThat(slot).as("round=%d key=%s", round, key).isNotEqualTo(-1);
                slots.add(slot);
            }
            assertThat(slots).hasSize(n);
            assertThat(hash.lookup("absent-" + round)).isEqualTo(-1);
        }
    }

    @Test
    void shouldBeSeedDeterministicAndFailFast() {
        List<String> keys = List.of("alpha", "beta", "gamma", "delta");
        PerfectHash first = PerfectHash.build(keys, 99);
        PerfectHash second = PerfectHash.build(keys, 99);
        for (String key : keys) {
            assertThat(first.lookup(key)).isEqualTo(second.lookup(key));
        }
        assertThatThrownBy(() -> PerfectHash.build(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PerfectHash.build(List.of(), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PerfectHash.build(java.util.Arrays.asList("a", null), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PerfectHash.build(List.of("a", "a"), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PerfectHash.build(keys, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
