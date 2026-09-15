package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1701 / T2604：EvalOrderRotator 纯函数直测——确定性/多重集守恒/
 * 消序性/边界安全。
 */
class EvalOrderRotatorTest {

    @Test
    void sameRunIndexIsDeterministic() {
        var first = EvalOrderRotator.permutation(8, 3);
        var second = EvalOrderRotator.permutation(8, 3);
        assertThat(second.permutation()).containsExactlyElementsOf(first.permutation());
    }

    @Test
    void permutationPreservesMultiset() {
        var plan = EvalOrderRotator.permutation(8, 5);
        assertThat(plan.permutation()).containsExactlyInAnyOrder(0, 1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    void distinctRunIndicesProduceDistinctOrders() {
        List<List<Integer>> seen = new ArrayList<>();
        for (int runIndex = 0; runIndex < 8; runIndex++) {
            seen.add(EvalOrderRotator.permutation(8, runIndex).permutation());
        }
        for (int i = 0; i < seen.size(); i++) {
            for (int j = i + 1; j < seen.size(); j++) {
                assertThat(seen.get(j)).as("run %d vs %d", i, j).isNotEqualTo(seen.get(i));
            }
        }
    }

    @Test
    void shuffledPreservesContentAndDoesNotMutateInput() {
        List<String> items = new ArrayList<>(List.of("a", "b", "c", "d", "e", "f", "g"));
        List<String> rotated = EvalOrderRotator.shuffled(items, 2);
        assertThat(rotated).containsExactlyInAnyOrderElementsOf(items);
        assertThat(items).containsExactly("a", "b", "c", "d", "e", "f", "g");
        assertThat(EvalOrderRotator.shuffled(items, 2)).isEqualTo(rotated);
    }

    @Test
    void degenerateSizesAreSafe() {
        assertThat(EvalOrderRotator.shuffled(List.of(), 0)).isEmpty();
        assertThat(EvalOrderRotator.shuffled(List.of("only"), 7)).containsExactly("only");
        assertThat(EvalOrderRotator.shuffled(null, 1)).isEmpty();
        assertThat(EvalOrderRotator.permutation(0, 0).permutation()).isEmpty();
    }
}
