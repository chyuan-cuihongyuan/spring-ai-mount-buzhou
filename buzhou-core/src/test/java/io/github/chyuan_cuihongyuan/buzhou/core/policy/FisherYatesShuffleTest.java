package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5002 / T6106：Fisher-Yates 合同——排列均匀性、双射、
 * 种子确定性、不可变变体、畸形 fail-fast。
 */
class FisherYatesShuffleTest {

    @Test
    void permutationsShouldBeUniformOverManySeeds() {
        Map<String, Integer> counts = new TreeMap<>();
        int runs = 6000;
        for (long seed = 0; seed < runs; seed++) {
            List<String> shuffled = FisherYatesShuffle.shuffled(List.of("a", "b", "c"), seed);
            String signature = String.join("", shuffled);
            counts.merge(signature, 1, Integer::sum);
        }
        assertThat(counts).hasSize(6);   // 六排列全部出现
        for (Integer count : counts.values()) {
            assertThat(count).as("排列频次 %s（期望 1000）", count).isBetween(700, 1300);
        }
    }

    @Test
    void indexPermutationShouldBeBijective() {
        int[] indices = FisherYatesShuffle.shuffledIndices(64, 42L);
        Arrays.sort(indices);
        for (int i = 0; i < indices.length; i++) {
            assertThat(indices[i]).isEqualTo(i);
        }
    }

    @Test
    void sameSeedShouldReplaySamePermutation() {
        List<Integer> items = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> first = FisherYatesShuffle.shuffled(items, 77L);
        List<Integer> second = FisherYatesShuffle.shuffled(items, 77L);
        assertThat(first).isEqualTo(second);
        assertThat(new Random(77L).nextInt(3)).isEqualTo(new Random(77L).nextInt(3));
    }

    @Test
    void shuffledVariantShouldNotTouchOriginal() {
        List<String> original = List.of("a", "b", "c", "d", "e");
        List<String> shuffled = FisherYatesShuffle.shuffled(original, 9L);
        assertThat(shuffled).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
        assertThat(original).containsExactly("a", "b", "c", "d", "e");
    }

    @Test
    void invalidInputsShouldFailFast() {
        assertThatThrownBy(() -> FisherYatesShuffle.shuffled(null, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FisherYatesShuffle.shuffleInPlace(null, new Random(1L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FisherYatesShuffle.shuffledIndices(-1, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
